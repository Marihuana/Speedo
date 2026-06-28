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
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.data.sensor.lean.LeanDiagnosticLogger
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.model.OverlayMode
import kr.yooreka.speedo.domain.model.OverlaySettings
import kr.yooreka.speedo.domain.model.OverlaySize
import kr.yooreka.speedo.domain.model.SubscriptionPlan
import kr.yooreka.speedo.domain.repository.BillingRepository
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import java.io.File
import javax.inject.Inject

data class SettingsState(
    val speedUnit: String = "KM/H",
    val isCalibrating: Boolean = false,
    val isAdRemoved: Boolean = false,
    val leanMeasurementMode: LeanMode = LeanMode.DEFAULT,
    // 주행 종료 예상 감지 임계값(분, 0=OFF, F-18a).
    val autoStopThresholdMin: Int = 5,
    // 광고 제거 구독 플랜(월간/연간, 무료체험 포함).
    val subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    // 플로팅 오버레이 위젯 설정(F-19a/b).
    val overlaySettings: OverlaySettings = OverlaySettings(),
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
                billingRepository.subscriptionPlans,
                userPreferencesRepository.overlaySettingsFlow,
            ) { prefs, calibrating, isAdRemoved, plans, overlay ->
                SettingsState(
                    speedUnit = prefs.speedUnit,
                    isCalibrating = calibrating,
                    isAdRemoved = isAdRemoved,
                    leanMeasurementMode = LeanMode.fromName(prefs.leanMeasurementMode),
                    autoStopThresholdMin = prefs.autoStopThresholdMin,
                    subscriptionPlans = plans,
                    overlaySettings = overlay,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsState(),
            )

        /** 선택한 구독 플랜(월간/연간)의 결제 플로우를 시작한다. */
        fun purchasePlan(
            activity: Activity,
            plan: SubscriptionPlan,
        ) {
            billingRepository.launchBillingFlow(activity, plan)
        }

        fun updateSpeedUnit(unit: String) {
            viewModelScope.launch {
                userPreferencesRepository.updateSpeedUnit(unit)
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

        /** 주행 종료 예상 감지 임계값(분, 0=OFF) 변경(F-18a). */
        fun updateAutoStopThreshold(minutes: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updateAutoStopThreshold(minutes)
            }
        }

        /** 오버레이 사용 여부 변경(F-19). 권한 확인은 UI 레이어 책임. */
        fun updateOverlayEnabled(enabled: Boolean) {
            viewModelScope.launch {
                userPreferencesRepository.updateOverlayEnabled(enabled)
            }
        }

        /** 오버레이 표시 모드 변경(F-19a). */
        fun updateOverlayMode(mode: OverlayMode) {
            viewModelScope.launch {
                userPreferencesRepository.updateOverlayMode(mode)
            }
        }

        /** 오버레이 크기 변경(F-19b). */
        fun updateOverlaySize(size: OverlaySize) {
            viewModelScope.launch {
                userPreferencesRepository.updateOverlaySize(size)
            }
        }

        /** 오버레이 투명도(0~100) 변경(F-19b). */
        fun updateOverlayOpacity(opacity: Int) {
            viewModelScope.launch {
                userPreferencesRepository.updateOverlayOpacity(opacity)
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
