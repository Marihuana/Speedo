package kr.yooreka.speedo.domain.model

/**
 * 차체 기울기(lean) 측정 방식. 설정에서 선택 가능하며 [kr.yooreka.speedo.domain.repository.LeanProvider]
 * 전략을 런타임에 교체한다(F-03). 실주행 측정 후 가장 정확한 방식을 채택하기 위한 비교군이다.
 */
enum class LeanMode {
    /** TYPE_GRAVITY + atan2. 현행 기본값. */
    GRAVITY_TILT,

    /** TYPE_ACCELEROMETER + atan2. 센서융합 없는 원시 비교군. */
    ACCEL_TILT,

    /** TYPE_ROTATION_VECTOR(가속도+자이로+자력계 융합) → roll. */
    ROTATION_VECTOR,

    /** TYPE_GAME_ROTATION_VECTOR(가속도+자이로, 자력계 제외) → roll. 바이크 자기 노이즈에 강함. */
    GAME_ROTATION_VECTOR,

    /** 자이로 roll-rate 적분 + 직선 구간 중력 drift 보정(상보 필터). */
    COMPLEMENTARY,

    ;

    companion object {
        val DEFAULT: LeanMode = GRAVITY_TILT

        /** 영속 저장값(이름) 복원. 알 수 없는 값이면 기본값. */
        fun fromName(name: String?): LeanMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
