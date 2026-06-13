package kr.yooreka.speedo.ui.dashboard

import androidx.compose.ui.graphics.Color
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.ui.theme.SlateText

/**
 * 대시보드 화면의 UI 상태
 */
data class DashBoardState(
    val speed: String = "0",
    val roll: String = "0°",
    val brakeEvent: BrakeEvent = BrakeEvent.NONE,
    val rearPressure: String = "0",
    val frontPressure: String = "0",
    val rearTemp: String = "0°",
    val frontTemp: String = "0°",
    val rearBat: String = "0.0V",
    val frontBat: String = "0.0V",
    val rearPressureColor: Color = SlateText,
    val frontPressureColor: Color = SlateText,
    val isRecording: Boolean = false,
    val speedUnit: String = "KM/H",
    val pressureUnit: String = "PSI",
    val showTpmsData: Boolean = false,
)
