package kr.yooreka.speedo.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.billing.BillingRepository
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.domain.usecase.GetDashboardTelemetryUseCase
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

        // TPMS 는 이번 버전 비활성화(F-07): BLE 스캔을 시작하지 않고 대시보드에 노출하지 않는다.
        // 백엔드(TpmsRepository/DataSource)와 데이터 모델은 추후 재도입을 위해 보존한다.
        init {
            getDashboardTelemetryUseCase.start()
        }

        @OptIn(kotlinx.coroutines.FlowPreview::class)
        val uiState: StateFlow<DashBoardState> =
            combine(
                getDashboardTelemetryUseCase().sample(100L),
                telemetryRepository.isRecording,
                userPreferencesRepository.userPreferencesFlow,
            ) { data, recording, prefs ->
                // Convert speed if unit is MPH
                val displaySpeed =
                    if (prefs.speedUnit == "MPH") {
                        (data.speed * 0.621371f).toInt()
                    } else {
                        data.speed.toInt()
                    }

                // TPMS 관련 필드(showTpmsData/압력/온도/전압/색상)는 채우지 않는다(비활성화, 기본값 유지).
                DashBoardState(
                    speed = displaySpeed.toString(),
                    roll = "${data.roll.toInt()}°",
                    brakeEvent = data.brakeEvent,
                    isRecording = recording,
                    speedUnit = prefs.speedUnit,
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

        override fun onCleared() {
            super.onCleared()
            getDashboardTelemetryUseCase.stop()
        }
    }
