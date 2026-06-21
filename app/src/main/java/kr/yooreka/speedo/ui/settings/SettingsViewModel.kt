package kr.yooreka.speedo.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.billing.BillingRepository
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.data.sensor.lean.LeanDiagnosticLogger
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val showTpmsData: Boolean = false,
    val speedUnit: String = "KM/H",
    val pressureUnit: String = "PSI",
    val isCalibrating: Boolean = false,
    val hasSavedTpmsIds: Boolean = false,
    val isAdRemoved: Boolean = false,
    val leanMeasurementMode: LeanMode = LeanMode.DEFAULT,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val calibrationRepository: LeanCalibrationRepository,
        private val billingRepository: BillingRepository,
        private val leanDiagnosticLogger: LeanDiagnosticLogger,
    ) : ViewModel() {
        private val isCalibratingFlow = MutableStateFlow(false)

        val uiState: StateFlow<SettingsState> =
            combine(
                userPreferencesRepository.userPreferencesFlow,
                isCalibratingFlow,
                billingRepository.isAdRemoved,
            ) { prefs, calibrating, isAdRemoved ->
                SettingsState(
                    showTpmsData = prefs.showTpmsData,
                    speedUnit = prefs.speedUnit,
                    pressureUnit = prefs.pressureUnit,
                    isCalibrating = calibrating,
                    hasSavedTpmsIds = prefs.frontTpmsId.isNotBlank() && prefs.rearTpmsId.isNotBlank(),
                    isAdRemoved = isAdRemoved,
                    leanMeasurementMode = LeanMode.fromName(prefs.leanMeasurementMode),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsState(),
            )

        fun purchaseRemoveAds(activity: Activity) {
            billingRepository.launchBillingFlow(activity)
        }

        fun toggleTpms(enabled: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.updateShowTpmsData(enabled)
            }
        }

        fun updateSpeedUnit(unit: String) {
            viewModelScope.launch {
                userPreferencesRepository.updateSpeedUnit(unit)
            }
        }

        fun updatePressureUnit(unit: String) {
            viewModelScope.launch {
                userPreferencesRepository.updatePressureUnit(unit)
            }
        }

        /** lean 측정 방식(F-03)을 변경한다. 활성 전략이 즉시 교체된다. */
        fun updateLeanMeasurementMode(mode: LeanMode) {
            viewModelScope.launch {
                userPreferencesRepository.updateLeanMeasurementMode(mode)
            }
        }

        /** 저장된 lean 진단 CSV 파일 목록(Export 메일 전송용). 기록은 주행 측정 중 자동 수행된다. */
        fun diagnosticCsvFiles(): List<File> = leanDiagnosticLogger.logFiles()

        fun saveTpmsIds(
            frontId: String,
            rearId: String,
        ) {
            viewModelScope.launch {
                userPreferencesRepository.updateTpmsIds(frontId, rearId)
            }
        }

        /** 저장된 TPMS 센서 ID(앞/뒤)를 초기화한다. */
        fun resetTpmsIds() {
            viewModelScope.launch {
                userPreferencesRepository.resetTpmsIds()
            }
        }

        /** 영점 보정값을 초기화한다(offset = 0). */
        fun resetCalibration() {
            calibrationRepository.reset()
        }

        /** 현재 차체 기울기를 영점으로 보정한다. (보정값은 앱 종료 시까지 유지) */
        fun calibrate() {
            if (isCalibratingFlow.value) return
            viewModelScope.launch {
                isCalibratingFlow.value = true
                try {
                    calibrationRepository.calibrate()
                } finally {
                    isCalibratingFlow.value = false
                }
            }
        }
    }
