package kr.yooreka.speedo.data.sensor.repository

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.yooreka.speedo.R
import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.local.entity.RideEntity
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.data.sensor.datasource.YawRateProvider
import kr.yooreka.speedo.data.sensor.lean.LeanDiagnosticLogger
import kr.yooreka.speedo.domain.model.AccelerometerData
import kr.yooreka.speedo.domain.model.BrakeDetector
import kr.yooreka.speedo.domain.model.BrakeDetector.BrakeState
import kr.yooreka.speedo.domain.model.LeanConfidence
import kr.yooreka.speedo.domain.model.LeanPhysicsGuard
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.service.RecordingService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TelemetryRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val accelRepo: SensorRepository<AccelerometerData>,
        private val leanMeasurement: LeanMeasurement,
        private val locationRepo: SensorRepository<LocationData>,
        private val telemetryDao: TelemetryDao,
        private val rideDao: RideDao,
        private val calibrationRepository: LeanCalibrationRepository,
        private val leanDiagnosticLogger: LeanDiagnosticLogger,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val yawRateProvider: YawRateProvider,
    ) : TelemetryRepository {
        private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val buffer = mutableListOf<TelemetryEntity>()

        private var currentRideId: Long = -1
        private var maxLeanForSession: Float = 0f
        private var maxSpeedForSession: Float = 0f
        private var startTimestamp: Long = 0

        // 구간 거리는 안드로이드의 WGS84 기반 Location.distanceBetween 으로 계산한다.
        // GPS 지터 완화: 2m 미만 미세 이동 무시 + 정확도 25m 초과 측위 무시.
        private val distanceTracker =
            RideDistanceTracker(
                minDistanceMeters = MIN_SEGMENT_METERS,
                maxAccuracyMeters = MAX_ACCURACY_METERS,
            ) { lat1, lng1, lat2, lng2 ->
                val results = FloatArray(1)
                Location.distanceBetween(lat1, lng1, lat2, lng2, results)
                results[0]
            }

        // 구간 내 절대값이 가장 큰 기울기(부호 보존)와 그 샘플의 신뢰도(F-03b). 200ms 타이머가
        // getAndSet 으로 소비하므로 read-then-reset 레이스를 피하기 위해 AtomicReference 로 둔다.
        private val maxLeanInInterval = AtomicReference(LeanSample.EMPTY)

        // 구간 내 가장 강한 제동(F-10). 200ms 틱 사이의 피크 제동이 누락되지 않도록 누적한다.
        private val maxBrakeInInterval = AtomicReference(BrakeState())

        // 새 GPS fix 가 들어온 시점의 좌표를 잠시 보관(dirty-flag). 다음 200ms 틱이 한 번만
        // 소비(getAndSet(null))하여 그 행에만 실좌표를 넣고, 이후 틱은 위치 null 로 기록한다(F-13c 보간 대비).
        private val pendingLocation = AtomicReference<Pair<Double, Double>?>(null)

        private val _isRecording = MutableStateFlow(false)
        override val isRecording: Flow<Boolean> = _isRecording.asStateFlow()

        // 주행 종료 예상 감지(F-18). 저속(< AUTO_STOP_SPEED_KMH)이 임계 시간 지속되면 true.
        private val _autoStopSuggested = MutableStateFlow(false)
        override val autoStopSuggested: StateFlow<Boolean> = _autoStopSuggested.asStateFlow()

        // 감지 임계값(분, 0=OFF). 설정(F-18a)을 상시 반영한다.
        @Volatile
        private var autoStopThresholdMin = UserPreferencesRepository.DEFAULT_AUTO_STOP_MIN

        // 저속 구간이 시작된 시각(ms). 0이면 현재 저속 구간 아님.
        @Volatile
        private var lowSpeedSinceMs = 0L

        init {
            repositoryScope.launch {
                userPreferencesRepository.autoStopThresholdFlow.collect { autoStopThresholdMin = it }
            }
        }

        // 가속도 스트림을 상시 scan 하여 최신 제동 판정 상태를 보유한다.
        // 대시보드(GetDashboardTelemetryUseCase)와 기록(200ms 타이머)이 동일 소스를 쓴다.
        override val brakeStream: StateFlow<BrakeState> =
            accelRepo.dataStream
                .scan(BrakeState(), BrakeDetector::reduce)
                .stateIn(
                    scope = repositoryScope,
                    started = SharingStarted.Eagerly,
                    initialValue = BrakeState(),
                )

        private fun addToBuffer(entity: TelemetryEntity) {
            synchronized(buffer) {
                buffer.add(entity)

                if (buffer.size >= BUFFER_SIZE) {
                    flushBuffer()
                }
            }
        }

        override fun startTelemetry() {
            accelRepo.start()
            leanMeasurement.start()
            locationRepo.start()
            // 뱅킹각 물리 가드(F-03b)용 yaw 요율 산출 시작. accel 가동 후 시작해 위 벡터를 확보한다.
            yawRateProvider.start()
        }

        override fun stopTelemetry() {
            stopRecording()
            accelRepo.stop()
            leanMeasurement.stop()
            locationRepo.stop()
            yawRateProvider.stop()
        }

        private var recordingJob: Job? = null

        override fun startRecording() {
            if (_isRecording.value) return

            // Start Foreground Service
            val serviceIntent =
                Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                }
            ContextCompat.startForegroundService(context, serviceIntent)

            // 주행 기록 중에만 lean 진단 CSV를 기록한다(F-03). 기록 시작 시 새 세션 파일 생성.
            leanDiagnosticLogger.start()

            repositoryScope.launch {
                val now = System.currentTimeMillis()
                val datePattern = context.getString(R.string.ride_title_date_format)
                val dateStr = SimpleDateFormat(datePattern, Locale.getDefault()).format(Date(now))
                val newRide =
                    RideEntity(
                        title = context.getString(R.string.default_ride_title, dateStr),
                        startTime = now,
                    )
                currentRideId = rideDao.insertRide(newRide)
                startTimestamp = now
                maxLeanForSession = 0f
                maxSpeedForSession = 0f
                maxLeanInInterval.set(LeanSample.EMPTY)
                maxBrakeInInterval.set(BrakeState())
                pendingLocation.set(null)
                distanceTracker.reset()
                lowSpeedSinceMs = 0L
                _autoStopSuggested.value = false
                _isRecording.value = true

                // 활성 측정 전략(F-03): 구간 내 부호 보존 최대 기울기와 세션 최대 기울기를 누적한다(행 생성과 무관).
                // 물리적 뱅킹각 가드(F-03b)로 폰 조작 등 과대 노이즈를 보정한 값으로 누적한다.
                val aggregatorJob =
                    launch {
                        leanMeasurement.leanStream.collect { lean ->
                            if (lean.isNaN()) return@collect
                            val rawRoll = lean - calibrationRepository.offsetDegrees.value
                            val guard =
                                LeanPhysicsGuard.evaluate(
                                    rawRollDeg = rawRoll,
                                    speedKmh = locationRepo.dataStream.value.speed,
                                    yawRateRadPerSec = yawRateProvider.yawRateStream.value,
                                )
                            val signedRoll = guard.roll
                            val magnitude = abs(signedRoll)
                            // 구간 내 절대값이 가장 큰 기울기를 '부호까지' 보존해 저장한다(좌/우 방향 유지).
                            maxLeanInInterval.getAndUpdate { current ->
                                if (magnitude > abs(current.roll)) LeanSample(signedRoll, guard.confidence) else current
                            }
                            // 세션 최대 기울기는 요약(RideEntity.maxLean)용. PRD §4.1: VALID 데이터만 집계하여
                            // 극저속(LOW_SPEED_UNRELIABLE)·이상치(OUTLIER_NOISE)가 최대 뱅킹각을 오염시키지 않게 한다.
                            if (guard.confidence == LeanConfidence.VALID && magnitude > maxLeanForSession) {
                                maxLeanForSession = magnitude
                            }
                        }
                    }

                // 제동 누적(F-10): 구간 내 가장 강한 제동을 보존해 200ms 틱 사이 피크 누락을 막는다.
                val brakeAggregatorJob =
                    launch {
                        brakeStream.collect { brake ->
                            maxBrakeInInterval.getAndUpdate { current ->
                                if (brake.force > current.force) brake else current
                            }
                        }
                    }

                // GPS: 거리 누적 / 세션 최고 속도 갱신 / 다음 시간주기 행에 채울 좌표(pending) 세팅만 담당.
                val locationJob =
                    launch {
                        locationRepo.dataStream.collect { locationData ->
                            // (0,0) 좌표는 fix 없음으로 간주하고 무시한다.
                            if (locationData.latitude == 0.0 && locationData.longitude == 0.0) return@collect

                            // Accumulate distance (직전 좌표와의 구간 거리 누적, 정확도 게이트 적용)
                            distanceTracker.add(locationData.latitude, locationData.longitude, locationData.accuracy)

                            // Track session max speed (km/h)
                            if (locationData.speed > maxSpeedForSession) {
                                maxSpeedForSession = locationData.speed
                            }

                            // 새 fix 시점의 좌표를 보관. 다음 200ms 틱 한 번만 이 좌표를 소비한다(F-13c 보간 대비).
                            pendingLocation.set(locationData.latitude to locationData.longitude)
                        }
                    }

                // 200ms(5Hz) 시간주기 타이머: GPS 콜백과 무관하게 기울기/제동 행을 주기 저장한다.
                val periodicJob =
                    launch {
                        while (isActive) {
                            delay(LOG_INTERVAL_MS)

                            // 구간 최대 기울기/제동을 소비(읽고 리셋)한다(F-03b 신뢰도·F-10 피크 보존).
                            val leanToSave = maxLeanInInterval.getAndSet(LeanSample.EMPTY)
                            val brake = maxBrakeInInterval.getAndSet(BrakeState())
                            // 이 틱에 새 GPS fix 좌표가 있으면 한 번만 소비, 없으면 위치 null.
                            val fix = pendingLocation.getAndSet(null)
                            val speedNow = locationRepo.dataStream.value.speed

                            // 주행 종료 예상 감지(F-18): 저속 지속 시간 추적.
                            checkAutoStop(speedNow)

                            addToBuffer(
                                TelemetryEntity(
                                    rideId = currentRideId,
                                    timestamp = System.currentTimeMillis(),
                                    speed = speedNow,
                                    roll = leanToSave.roll,
                                    brakeEvent = brake.event,
                                    brakeForce = brake.force,
                                    latitude = fix?.first,
                                    longitude = fix?.second,
                                    leanConfidence = leanToSave.confidence,
                                ),
                            )
                        }
                    }

                recordingJob =
                    launch {
                        aggregatorJob.join()
                        brakeAggregatorJob.join()
                        locationJob.join()
                        periodicJob.join()
                    }
            }
        }

        override fun stopRecording() {
            if (!_isRecording.value) return

            // Stop Foreground Service
            val serviceIntent =
                Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
            ContextCompat.startForegroundService(context, serviceIntent)

            val rideId = currentRideId
            val endTime = System.currentTimeMillis()
            val maxLean = maxLeanForSession
            val maxSpeed = maxSpeedForSession
            val distanceKm = distanceTracker.totalKm
            val duration = endTime - startTimestamp

            _isRecording.value = false
            recordingJob?.cancel()
            recordingJob = null
            currentRideId = -1
            lowSpeedSinceMs = 0L
            _autoStopSuggested.value = false

            // 진단 CSV 세션 마감(파일 flush/close).
            leanDiagnosticLogger.stop()

            repositoryScope.launch {
                flushBuffer()
                val ride = rideDao.getRideById(rideId)
                ride?.let {
                    rideDao.updateRide(
                        it.copy(
                            endTime = endTime,
                            maxLean = maxLean,
                            maxSpeed = maxSpeed,
                            totalDistance = distanceKm,
                            duration = duration,
                        ),
                    )
                }
            }
        }

        override fun continueRide() {
            // '계속' 선택: 감지 타이머 초기화. 이후 다시 임계 시간 저속이면 재발행한다(§4.7).
            lowSpeedSinceMs = 0L
            _autoStopSuggested.value = false
        }

        /**
         * 주행 종료 예상 감지(F-18). 저속(< [AUTO_STOP_SPEED_KMH], 정지+도보 포함)이 임계 시간(분) 지속되면
         * [autoStopSuggested] 를 true 로 만든다. 속도가 회복되면 타이머와 제안을 모두 리셋한다.
         */
        private fun checkAutoStop(speedKmh: Float) {
            val thresholdMin = autoStopThresholdMin
            if (thresholdMin <= 0) {
                // OFF: 감지 비활성.
                lowSpeedSinceMs = 0L
                return
            }
            if (speedKmh >= AUTO_STOP_SPEED_KMH) {
                // 주행 재개 → 타이머/제안 리셋.
                lowSpeedSinceMs = 0L
                _autoStopSuggested.value = false
                return
            }
            val now = System.currentTimeMillis()
            if (lowSpeedSinceMs == 0L) {
                lowSpeedSinceMs = now
            } else if (!_autoStopSuggested.value && now - lowSpeedSinceMs >= thresholdMin * MILLIS_PER_MIN) {
                _autoStopSuggested.value = true
            }
        }

        override fun flushBuffer() {
            val snapshot =
                synchronized(buffer) {
                    if (buffer.isEmpty()) return
                    val items = buffer.toList()
                    buffer.clear()
                    items
                }

            repositoryScope.launch {
                telemetryDao.insertAll(snapshot)
            }
        }

        /** 구간 최대 기울기 샘플(부호 보존 roll + 신뢰도). 200ms 타이머가 한 번에 소비한다(F-03b). */
        private data class LeanSample(
            val roll: Float,
            val confidence: LeanConfidence,
        ) {
            companion object {
                val EMPTY = LeanSample(0f, LeanConfidence.VALID)
            }
        }

        companion object {
            /** 텔레메트리 시간주기 저장 간격(ms). 5Hz(200ms) 고정. PRD §4.1 기본값. */
            private const val LOG_INTERVAL_MS = 200L

            /** 텔레메트리 버퍼를 DB에 플러시하는 임계 크기. */
            private const val BUFFER_SIZE = 100

            /** 이보다 작은 구간 이동은 GPS 지터로 보고 거리에 더하지 않는다. */
            private const val MIN_SEGMENT_METERS = 2f

            /** 정확도가 이보다 나쁜(큰) 측위는 거리 계산에서 제외한다. */
            private const val MAX_ACCURACY_METERS = 25f

            /** 주행 종료 예상 감지(F-18): 이 속도(km/h) 미만이면 정지/도보로 간주(< 7km/h, 끌바·도보 포함). */
            private const val AUTO_STOP_SPEED_KMH = 7f

            private const val MILLIS_PER_MIN = 60_000L
        }
    }
