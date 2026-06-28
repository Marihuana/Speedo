package kr.yooreka.speedo.domain.model

/**
 * 플로팅 오버레이 위젯 설정(F-19a/F-19b).
 *
 * @property enabled 오버레이 사용 여부.
 * @property mode 표시 모드(속도/기울기/복합).
 * @property size 위젯 크기(소/중/대).
 * @property opacity 위젯 투명도(0~100, 100=불투명).
 */
data class OverlaySettings(
    val enabled: Boolean = false,
    val mode: OverlayMode = OverlayMode.SPEEDOMETER,
    val size: OverlaySize = OverlaySize.MEDIUM,
    val opacity: Int = 100,
)
