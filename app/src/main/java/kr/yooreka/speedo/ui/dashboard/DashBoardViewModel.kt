package kr.yooreka.speedo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.repository.BillingRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.domain.usecase.GetDashboardTelemetryUseCase
import kr.yooreka.speedo.utils.displaySpeedInt
import kr.yooreka.speedo.utils.formatLeanAngle
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
    ) : ViewModel() {
        private val _uiEvent = MutableSharedFlow<DashBoardUiEvent>()
        val uiEvent = _uiEvent.asSharedFlow()

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
                        // 물리 가드(F-03b) 판정: 극저속 정지 노이즈/이상치는 최대 뱅킹각 누적에서 제외하고,
                        // 저속이라도 선회 중이면 포함한다.
                        if (!data.countsTowardMax) return@collect
                        val roll = data.roll
                        when {
                            roll > 0f -> if (roll > maxLeftRoll.value) maxLeftRoll.value = roll
                            roll < 0f -> if (-roll > maxRightRoll.value) maxRightRoll.value = -roll
                        }
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
            ) { data, recording, prefs, autoStop, maxRoll ->
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

        /** 주행 종료 예상 다이얼로그 '아니오'(계속): 감지 타이머만 초기화하고 기록 유지(F-18). */
        fun onAutoStopContinue() {
            telemetryRepository.continueRide()
        }

        /** 주행 종료 예상 다이얼로그 '예'(종료): 기록을 종료한다(+광고 노출). */
        fun onAutoStopConfirm() {
            viewModelScope.launch {
                getDashboardTelemetryUseCase.stopRecording()
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
