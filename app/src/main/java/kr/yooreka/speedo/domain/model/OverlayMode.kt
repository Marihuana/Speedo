package kr.yooreka.speedo.domain.model

/**
 * 플로팅 오버레이 위젯 표시 모드(F-19a).
 * - [SPEEDOMETER]: 속도/제동 위젯
 * - [LEAN]: 기울기 전용 위젯
 * - [COMBINED]: 속도+기울기 통합 위젯
 */
enum class OverlayMode {
    SPEEDOMETER,
    LEAN,
    COMBINED,
    ;

    companion object {
        fun fromName(name: String?): OverlayMode = entries.firstOrNull { it.name == name } ?: SPEEDOMETER
    }
}
