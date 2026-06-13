package kr.yooreka.speedo.ui.dashboard

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.yooreka.speedo.domain.usecase.GetDashboardTelemetryUseCase
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.model.TpmsData
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.ui.theme.DangerRed
import kr.yooreka.speedo.ui.theme.GreenSuccess
import kr.yooreka.speedo.ui.theme.SlateText
import kr.yooreka.speedo.data.billing.BillingRepository
import kr.yooreka.speedo.ui.theme.WarningYellow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class DashBoardUiEvent {
    object ShowStartDialog : DashBoardUiEvent()
    object ShowInterstitialAd : DashBoardUiEvent()
}

@HiltViewModel
class DashBoardViewModel @Inject constructor(
    private val getDashboardTelemetryUseCase: GetDashboardTelemetryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val tpmsRepository: SensorRepository<TpmsData>,
    private val telemetryRepository: TelemetryRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<DashBoardUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        getDashboardTelemetryUseCase.start()
        tpmsRepository.start()
    }

    private fun getPressureColor(current: Float, baseline: Float): Color {
        if (current <= 0f) return SlateText // Fallback when no data
        
        val diffPercent = ((current - baseline) / baseline) * 100f
        
        return when {
            diffPercent in -5f..15f -> GreenSuccess
            diffPercent in -15f..25f -> WarningYellow // This falls through if not in -5..15
            else -> DangerRed
        }
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<DashBoardState> = combine(
        getDashboardTelemetryUseCase().sample(100L),
        telemetryRepository.isRecording,
        userPreferencesRepository.userPreferencesFlow,
        tpmsRepository.dataStream
    ) { data, recording, prefs, tpms ->
        // Convert speed if unit is MPH
        val displaySpeed = if (prefs.speedUnit == "MPH") {
            (data.speed * 0.621371f).toInt()
        } else {
            data.speed.toInt()
        }

        // Calculate colors based on PSI baselines (Front: 36, Rear: 40)
        val frontColor = getPressureColor(tpms.frontPressurePsi, 36f)
        val rearColor = getPressureColor(tpms.rearPressurePsi, 40f)

        // Convert TPMS if unit is BAR, but keep checking logic in PSI
        val displayRear = if (prefs.pressureUnit == "BAR") String.format("%.1f", tpms.rearPressurePsi * 0.0689476f) else String.format("%.1f", tpms.rearPressurePsi)
        val displayFront = if (prefs.pressureUnit == "BAR") String.format("%.1f", tpms.frontPressurePsi * 0.0689476f) else String.format("%.1f", tpms.frontPressurePsi)

        DashBoardState(
            speed = displaySpeed.toString(),
            roll = "${data.roll.toInt()}°",
            brakeEvent = data.brakeEvent,
            isRecording = recording,
            speedUnit = prefs.speedUnit,
            pressureUnit = prefs.pressureUnit,
            showTpmsData = prefs.showTpmsData,
            rearPressure = displayRear,
            frontPressure = displayFront,
            rearTemp = "${tpms.rearTemperature.toInt()}°",
            frontTemp = "${tpms.frontTemperature.toInt()}°",
            rearBat = String.format("%.1fV", tpms.rearBatteryVoltage),
            frontBat = String.format("%.1fV", tpms.frontBatteryVoltage),
            rearPressureColor = rearColor,
            frontPressureColor = frontColor
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashBoardState()
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
        tpmsRepository.stop()
    }
}
