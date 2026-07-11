package kr.yooreka.speedo.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.data.sensor.lean.LeanDiagnosticLogger
import kr.yooreka.speedo.domain.model.LeanConfidence
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.repository.BillingRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.domain.usecase.GetDashboardTelemetryUseCase
import kr.yooreka.speedo.utils.displaySpeedInt
import kr.yooreka.speedo.utils.formatLeanAngle
import java.util.Locale
import javax.inject.Inject

sealed class DashBoardUiEvent {
    object ShowStartDialog : DashBoardUiEvent()

    object ShowInterstitialAd : DashBoardUiEvent()
}

@HiltViewModel
class DashBoardViewModel
    @Inject
    constructor(
        private val getDashboardTelemetryUseCase: GetDashboardTelemetryUseCase,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val telemetryRepository: TelemetryRepository,
        private val billingRepository: BillingRepository,
        private val leanDiagnosticLogger: LeanDiagnosticLogger,
        private val locationRepo: SensorRepository<LocationData>,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiEvent = MutableSharedFlow<DashBoardUiEvent>()
        val uiEvent = _uiEvent.asSharedFlow()

        private val keyStartTime = "start_time"
        private val keyLastLat = "last_lat"
        private val keyLastLng = "last_lng"
        private val keyAccumDistance = "accum_distance"

        private var startTimestamp: Long
            get() = savedStateHandle[keyStartTime] ?: 0L
            set(value) {
                savedStateHandle[keyStartTime] = value
            }

        private var lastLocation: Pair<Double, Double>?
            get() {
                val lat: Double = savedStateHandle[keyLastLat] ?: return null
                val lng: Double = savedStateHandle[keyLastLng] ?: return null
                return lat to lng
            }
            set(value) {
                savedStateHandle[keyLastLat] = value?.first
                savedStateHandle[keyLastLng] = value?.second
            }

        private var accumDistance: Double
            get() = savedStateHandle[keyAccumDistance] ?: 0.0
            set(value) {
                savedStateHandle[keyAccumDistance] = value
            }

        private val rideDurationFlow = MutableStateFlow("00:00:00")
        private val rideDistanceFlow = MutableStateFlow("0.0")

        // 세션 최대 기울기(F: MAX L/R). 부호 규약: 양수=좌(L), 음수=우(R).
        // 기록 중에만 누적하고 기록 시작 시 0 으로 초기화한다.
        private val maxLeftRoll = MutableStateFlow(0f)
        private val maxRightRoll = MutableStateFlow(0f)
        private val maxRollFlow = combine(maxLeftRoll, maxRightRoll) { left, right -> left to right }

        // TPMS 는 이번 버전 비활성화(F-07): BLE 스캔을 시작하지 않고 대시보드에 노출하지 않는다.
        // 백엔드(TpmsRepository/DataSource)와 데이터 모델은 추후 재도입을 위해 보존한다.
        init {
            getDashboardTelemetryUseCase.start()
            // 기록 중 세션 최대 좌/우 뱅킹각 누적. 기록이 멈추면 collectLatest 가 내부 수집을 취소한다.
            viewModelScope.launch {
                telemetryRepository.isRecording.collectLatest { recording ->
                    if (!recording) return@collectLatest
                    maxLeftRoll.value = 0f
                    maxRightRoll.value = 0f
                    getDashboardTelemetryUseCase().collect { data ->
                        // 물리 가드(F-03b): L/R 최대각은 VALID 데이터만 갱신한다. 극저속/이상치(정차 폰 조작 등)는
                        // 현재 뱅킹각엔 즉각 반영되지만 최대치 카드는 갱신하지 않는다.
                        if (data.leanConfidence != LeanConfidence.VALID) return@collect
                        val roll = data.roll
                        when {
                            roll > 0f -> if (roll > maxLeftRoll.value) maxLeftRoll.value = roll
                            roll < 0f -> if (-roll > maxRightRoll.value) maxRightRoll.value = -roll
                        }
                    }
                }
            }

            // 화면 회전 시에도 경과시간/누적거리 계산 복원 및 유지
            viewModelScope.launch {
                telemetryRepository.isRecording.collectLatest { recording ->
                    if (recording) {
                        if (startTimestamp == 0L) {
                            startTimestamp = System.currentTimeMillis()
                            accumDistance = 0.0
                            lastLocation = null
                        }
                        rideDistanceFlow.value = String.format(Locale.US, "%.1f", accumDistance)

                        // coroutineScope 블록을 활용하여 수집 취소 시 하위 코루틴들의 좀비 코루틴 누수 방지
                        coroutineScope {
                            // 1초 단위 타이머 루프
                            launch {
                                while (isActive) {
                                    val elapsedMs = System.currentTimeMillis() - startTimestamp
                                    val hours = elapsedMs / 3600000
                                    val minutes = (elapsedMs % 3600000) / 60000
                                    val seconds = (elapsedMs % 60000) / 1000
                                    rideDurationFlow.value = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                                    delay(1000L)
                                }
                            }

                            // GPS 누적 거리 루프 (F-06 정책: Accuracy <= 25m, Move >= 2m)
                            launch {
                                locationRepo.dataStream.collect { locationData ->
                                    if (locationData.latitude == 0.0 && locationData.longitude == 0.0) return@collect
                                    if (locationData.accuracy > 25f) return@collect

                                    val last = lastLocation
                                    if (last == null) {
                                        lastLocation = locationData.latitude to locationData.longitude
                                    } else {
                                        val results = FloatArray(1)
                                        android.location.Location.distanceBetween(
                                            last.first,
                                            last.second,
                                            locationData.latitude,
                                            locationData.longitude,
                                            results,
                                        )
                                        val distMeters = results[0]
                                        if (distMeters >= 2f) {
                                            accumDistance += (distMeters / 1000.0)
                                            rideDistanceFlow.value = String.format(Locale.US, "%.1f", accumDistance)
                                            lastLocation = locationData.latitude to locationData.longitude
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        startTimestamp = 0L
                        accumDistance = 0.0
                        lastLocation = null
                        rideDurationFlow.value = "00:00:00"
                        rideDistanceFlow.value = "0.0"
                    }
                }
            }
        }

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        val uiState: StateFlow<DashBoardState> =
            combine(
                getDashboardTelemetryUseCase().sample(100L),
                telemetryRepository.isRecording,
                userPreferencesRepository.userPreferencesFlow,
                telemetryRepository.autoStopSuggested,
                maxRollFlow,
                rideDurationFlow,
                rideDistanceFlow,
            ) { array ->
                val data = array[0] as kr.yooreka.speedo.domain.model.TelemetryData
                val recording = array[1] as Boolean
                val prefs = array[2] as kr.yooreka.speedo.data.local.preferences.UserPreferences
                val autoStop = array[3] as Boolean

                @Suppress("UNCHECKED_CAST")
                val maxRoll = array[4] as Pair<Float, Float>
                val duration = array[5] as String
                val distance = array[6] as String

                // 속도 단위 변환(F-02): 오버레이와 동일한 공용 변환점을 사용한다.
                val displaySpeed = displaySpeedInt(data.speed, prefs.speedUnit)

                // TPMS 관련 필드(showTpmsData/압력/온도/전압/색상)는 채우지 않는다(비활성화, 기본값 유지).
                DashBoardState(
                    speed = displaySpeed.toString(),
                    roll = formatLeanAngle(data.roll),
                    brakeEvent = data.brakeEvent,
                    isRecording = recording,
                    speedUnit = prefs.speedUnit,
                    autoStopSuggested = autoStop,
                    maxLeftRoll = formatLeanAngle(maxRoll.first),
                    maxRightRoll = formatLeanAngle(maxRoll.second),
                    rideDuration = duration,
                    rideDistance = distance,
                )
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = DashBoardState(),
                )

        fun toggleRecording() {
            viewModelScope.launch {
                // Check current state from repository flow
                val currentlyRecording = telemetryRepository.isRecording.first()
                if (currentlyRecording) {
                    getDashboardTelemetryUseCase.stopRecording()
                    if (!billingRepository.isAdRemoved.value) {
                        _uiEvent.emit(DashBoardUiEvent.ShowInterstitialAd)
                    }
                } else {
                    _uiEvent.emit(DashBoardUiEvent.ShowStartDialog)
                }
            }
        }

        fun onConfirmRecording() {
            getDashboardTelemetryUseCase.startRecording()
        }

        /** 대시보드 ⚠️ 이슈 제보(1.0 보완): 진단 로그에 오차 발생 시점을 마킹한다(테스터 진단용). */
        fun markDiagnosticIssue() {
            leanDiagnosticLogger.markIssue()
        }

        /** 주행 종료 예상 다이얼로그 '아니오'(계속): 감지 타이머만 초기화하고 기록 유지(F-18). */
        fun onAutoStopContinue() {
            telemetryRepository.continueRide()
        }

        /** 주행 종료 예상 다이얼로그 '예'(종료): 정차 시점 기준 Trim 저장 후 종료한다(+광고 노출). */
        fun onAutoStopConfirm() {
            viewModelScope.launch {
                getDashboardTelemetryUseCase.confirmAutoStop()
                if (!billingRepository.isAdRemoved.value) {
                    _uiEvent.emit(DashBoardUiEvent.ShowInterstitialAd)
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            getDashboardTelemetryUseCase.stop()
        }
    }
