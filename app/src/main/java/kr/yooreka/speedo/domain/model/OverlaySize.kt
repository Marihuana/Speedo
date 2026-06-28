package kr.yooreka.speedo.domain.model

/**
 * 플로팅 오버레이 위젯 크기(F-19b). [scale]은 위젯 기본 크기에 곱하는 배율이며,
 * 렌더 시 Density 배율로 적용해 레이아웃 크기까지 함께 변경한다.
 */
enum class OverlaySize(val scale: Float) {
    SMALL(0.75f),
    MEDIUM(1.0f),
    LARGE(1.25f),
    ;

    companion object {
        fun fromName(name: String?): OverlaySize = entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}
