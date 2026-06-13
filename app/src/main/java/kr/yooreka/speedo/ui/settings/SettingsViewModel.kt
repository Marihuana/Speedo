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
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import javax.inject.Inject

data class SettingsState(
    val showTpmsData: Boolean = false,
    val speedUnit: String = "KM/H",
    val pressureUnit: String = "PSI",
    val isCalibrating: Boolean = false,
    val hasSavedTpmsIds: Boolean = false,
    val isAdRemoved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val calibrationRepository: LeanCalibrationRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _isCalibrating = MutableStateFlow(false)

    val uiState: StateFlow<SettingsState> = combine(
        userPreferencesRepository.userPreferencesFlow,
        _isCalibrating,
        billingRepository.isAdRemoved
    ) { prefs, calibrating, isAdRemoved ->
        SettingsState(
            showTpmsData = prefs.showTpmsData,
            speedUnit = prefs.speedUnit,
            pressureUnit = prefs.pressureUnit,
            isCalibrating = calibrating,
            hasSavedTpmsIds = prefs.frontTpmsId.isNotBlank() && prefs.rearTpmsId.isNotBlank(),
            isAdRemoved = isAdRemoved
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsState()
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

    fun saveTpmsIds(frontId: String, rearId: String) {
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
        if (_isCalibrating.value) return
        viewModelScope.launch {
            _isCalibrating.value = true
            try {
                calibrationRepository.calibrate()
            } finally {
                _isCalibrating.value = false
            }
        }
    }
}
