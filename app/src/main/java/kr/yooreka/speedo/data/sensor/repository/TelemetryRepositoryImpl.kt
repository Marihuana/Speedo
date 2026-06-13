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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.R
import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.local.entity.RideEntity
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kr.yooreka.speedo.domain.model.AccelerometerData
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.service.RecordingService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class TelemetryRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val accelRepo: SensorRepository<AccelerometerData>,
        private val gravityRepo: SensorRepository<GravityData>,
        private val locationRepo: SensorRepository<LocationData>,
        private val telemetryDao: TelemetryDao,
        private val rideDao: RideDao,
        private val calibrationRepository: LeanCalibrationRepository,
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

        @Volatile private var maxLeanInInterval: Float = 0f

        private val _isRecording = MutableStateFlow(false)
        override val isRecording: Flow<Boolean> = _isRecording.asStateFlow()

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
            gravityRepo.start()
            locationRepo.start()
        }

        override fun stopTelemetry() {
            stopRecording()
            accelRepo.stop()
            gravityRepo.stop()
            locationRepo.stop()
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
                maxLeanInInterval = 0f
                distanceTracker.reset()
                _isRecording.value = true

                val aggregatorJob =
                    launch {
                        gravityRepo.dataStream.collect { gravity ->
                            val signedRoll = gravity.calibratedRoll(calibrationRepository.offsetDegrees.value)
                            val magnitude = abs(signedRoll)
                            // 구간 내 절대값이 가장 큰 기울기를 '부호까지' 보존해 저장한다(좌/우 방향 유지).
                            if (magnitude > abs(maxLeanInInterval)) {
                                maxLeanInInterval = signedRoll
                            }
                            // 세션 최대 기울기는 요약(RideEntity.maxLean)용이므로 크기(절대값)로 둔다.
                            if (magnitude > maxLeanForSession) {
                                maxLeanForSession = magnitude
                            }
                        }
                    }

                val locationJob =
                    launch {
                        locationRepo.dataStream.collect { locationData ->
                            // Skip if location is invalid
                            if (locationData.latitude == 0.0 && locationData.longitude == 0.0) return@collect

                            // Accumulate distance (직전 좌표와의 구간 거리 누적, 정확도 게이트 적용)
                            distanceTracker.add(locationData.latitude, locationData.longitude, locationData.accuracy)

                            // Track session max speed (km/h)
                            if (locationData.speed > maxSpeedForSession) {
                                maxSpeedForSession = locationData.speed
                            }

                            // Downsample: Get max lean from the last interval and reset it
                            val leanToSave = maxLeanInInterval
                            maxLeanInInterval = 0f

                            addToBuffer(
                                TelemetryEntity(
                                    rideId = currentRideId,
                                    timestamp = System.currentTimeMillis(),
                                    speed = locationData.speed,
                                    roll = leanToSave,
                                    brakeEvent = BrakeEvent.NONE,
                                    brakeForce = 0f,
                                    latitude = locationData.latitude,
                                    longitude = locationData.longitude,
                                ),
                            )
                        }
                    }

                recordingJob =
                    launch {
                        aggregatorJob.join()
                        locationJob.join()
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

        companion object {
            /** 텔레메트리 버퍼를 DB에 플러시하는 임계 크기. */
            private const val BUFFER_SIZE = 100

            /** 이보다 작은 구간 이동은 GPS 지터로 보고 거리에 더하지 않는다. */
            private const val MIN_SEGMENT_METERS = 2f

            /** 정확도가 이보다 나쁜(큰) 측위는 거리 계산에서 제외한다. */
            private const val MAX_ACCURACY_METERS = 25f
        }
    }
