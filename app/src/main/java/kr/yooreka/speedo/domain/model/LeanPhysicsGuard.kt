package kr.yooreka.speedo.domain.model

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sign

/**
 * 물리적 뱅킹각 가드(F-03b, PRD §4.1).
 *
 * 정상 원선회 모델로부터 자이로 요율(Yaw Rate) 기반 예측 뱅킹각을 계산해, 측정된 roll 이 물리적으로
 * 타당한지 판정한다. 정차/극저속에서 폰을 조작·탈착할 때 발생하는 과대 뱅킹 노이즈를 걸러낸다.
 *
 * ```
 * θ_expected = arctan(v · ω / g)
 * ```
 * (v: 속도 m/s, ω: 요율 rad/s, g: 9.81 m/s²)
 *
 * 판정:
 * - **극저속(v < [LOW_SPEED_KMH])**: 클리핑하지 않고 원시값 보존, [LeanConfidence.LOW_SPEED_UNRELIABLE].
 *   단, 최대 뱅킹각 집계 포함 여부([Result.includeInMax])는 '선회 중'일 때만 true 로 둔다 — yaw 요율이
 *   [LOW_SPEED_TURN_YAW_RAD_PER_SEC] 를 넘으면(저속 U턴·발 버팀 선회) 실제 기울기로 보고 포함하고,
 *   정지+무회전(폰 거치/조작 노이즈)은 제외한다.
 * - **yaw 데이터 없음(ω = NaN, 정상 속도)**: 검증 불가 → 원시값 보존(fail-open), 집계 포함.
 * - 그 외: 측정 roll 크기가 예측치를 속도 구간별 허용 오차([toleranceFor])보다 크게 초과하면
 *   [LeanConfidence.OUTLIER_NOISE] 로 보고 예측 크기로 클리핑(측정 부호 보존), 집계 제외. 이내면 [LeanConfidence.RELIABLE].
 *
 * 부호 규약: 측정 roll 의 부호(좌/우)는 그대로 유지하고 **크기만** 예측치로 제한한다. 기기 좌표계와
 * 차량 요율의 회전 방향 부호가 거치에 따라 달라질 수 있어, 크기 비교가 부호 비교보다 견고하다.
 *
 * 안드로이드 비의존 순수 로직으로 단위 테스트가 가능하다.
 */
object LeanPhysicsGuard {
    data class Result(
        val roll: Float,
        val confidence: LeanConfidence,
        /** 최대 뱅킹각 요약 집계에 포함할지 여부(극저속 정지 노이즈/이상치 제외). */
        val includeInMax: Boolean,
    )

    fun evaluate(
        rawRollDeg: Float,
        speedKmh: Float,
        yawRateRadPerSec: Float,
    ): Result {
        // 극저속 → 모델 신뢰 불가(요율 유무와 무관). 클리핑 없이 원시 보존하되 마킹.
        // yaw NaN 체크보다 먼저 둔다: 정차/극저속에서 yaw 데이터가 없어도 RELIABLE 로 새지 않게 한다.
        if (speedKmh < LOW_SPEED_KMH) {
            // '선회 중'(yaw 유의미)일 때만 최대각 집계에 포함 → 저속 선회 lean 은 살리고 정지 노이즈는 제외.
            val turning = !yawRateRadPerSec.isNaN() && abs(yawRateRadPerSec) > LOW_SPEED_TURN_YAW_RAD_PER_SEC
            return Result(rawRollDeg, LeanConfidence.LOW_SPEED_UNRELIABLE, includeInMax = turning)
        }

        // yaw 데이터 없음 → 검증 불가, 원시 보존(fail-open).
        if (yawRateRadPerSec.isNaN()) return Result(rawRollDeg, LeanConfidence.RELIABLE, includeInMax = true)

        val vMps = speedKmh / KMH_PER_MPS
        val expectedRad = atan((vMps * abs(yawRateRadPerSec) / GRAVITY).toDouble())
        val expectedMagDeg = Math.toDegrees(expectedRad).toFloat()

        // 측정 크기가 예측치를 허용 오차 이상 '초과'할 때만 노이즈로 본다(과대 보고 억제).
        // 측정이 예측보다 작은 경우는 폰 조작 오염 시나리오가 아니므로 보존한다.
        val excess = abs(rawRollDeg) - expectedMagDeg
        return if (excess > toleranceFor(speedKmh)) {
            Result(sign(rawRollDeg) * expectedMagDeg, LeanConfidence.OUTLIER_NOISE, includeInMax = false)
        } else {
            Result(rawRollDeg, LeanConfidence.RELIABLE, includeInMax = true)
        }
    }

    /** 속도 구간별 허용 오차(도). PRD §4.1. */
    private fun toleranceFor(speedKmh: Float): Float =
        when {
            speedKmh < 15f -> 20f
            speedKmh < 30f -> 15f
            else -> 12f
        }

    /** 이 속도(km/h) 미만은 극저속 예외 처리. */
    const val LOW_SPEED_KMH = 5f

    /** 극저속에서 '선회 중'으로 보는 yaw 요율 임계(rad/s, ≈8.6°/s). 이상이면 최대각 집계에 포함. */
    const val LOW_SPEED_TURN_YAW_RAD_PER_SEC = 0.15f

    private const val GRAVITY = 9.81f
    private const val KMH_PER_MPS = 3.6f
}
