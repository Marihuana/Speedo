package kr.yooreka.speedo.domain.model

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.sign

/**
 * 물리적 뱅킹각 가드(F-03b, PRD §4.1).
 *
 * 자이로 요율(Yaw Rate) 기반 예측 뱅킹각으로 측정 roll 의 물리적 타당성을 판정하여, 신뢰도
 * ([LeanConfidence])와 저장/렌더용 보정값([Result.roll])을 산출한다.
 *
 *     θ_expected = arctan(v · ω / g)   (v: m/s, ω: rad/s, g: 9.81)
 *
 * 판정:
 * - **극저속(v < [LOW_SPEED_KMH])** → [LeanConfidence.LOW_SPEED_UNRELIABLE].
 *   정차 시 폰 조작과 실제 정적 lean(스탠드 거치/발 버팀)을 구분할 수 없으므로, 보정값을 물리적으로
 *   타당한 상한 [LOW_SPEED_MAX_LEAN_DEG](±15°)로 클램프한다(≤15° 원시 유지, >15°는 15°로 제한).
 * - **yaw 없음(ω = NaN, v ≥ 5)** → 검증 불가, [LeanConfidence.VALID] (원시 보존, fail-open).
 * - **v ≥ 5**: |측정 roll| 이 예측치를 속도 구간별 허용 오차([toleranceFor]) 이상 초과하면
 *   [LeanConfidence.OUTLIER_NOISE](보정값 = 예측치로 클리핑, 부호 보존), 이내면 [LeanConfidence.VALID](원시).
 *
 * 부호 규약: 측정 roll 의 좌/우 부호는 유지하고 크기만 제한한다(기기-차량 좌표 부호가 거치에 따라
 * 달라질 수 있어 크기 비교가 부호 비교보다 견고).
 *
 * [Result.roll] 은 per-row 저장 및 지도/상세 렌더의 기준값이다. (단, 실시간 대시보드 '현재 뱅킹각'은
 * PRD §4.1 정책상 항상 원시값을 표시하므로 보정값을 쓰지 않는다. 지도에서 `OUTLIER_NOISE` 는 0°로
 * 평탄화 렌더하므로 소비처가 [Result.confidence] 를 함께 사용한다.)
 *
 * 안드로이드 비의존 순수 로직으로 단위 테스트가 가능하다.
 */
object LeanPhysicsGuard {
    data class Result(
        /** 저장/렌더용 보정 roll(극저속 ±15° 클램프, 이상치 예측치 클리핑, 그 외 원시). */
        val roll: Float,
        val confidence: LeanConfidence,
    )

    fun evaluate(
        rawRollDeg: Float,
        speedKmh: Float,
        yawRateRadPerSec: Float,
    ): Result {
        // 극저속: 모델 검증 불가. 보정값을 ±15° 로 클램프해 폰 조작 과대각은 억제하되 실제 정적 lean(≤15°)은 보존.
        if (speedKmh < LOW_SPEED_KMH) {
            val clamped = rawRollDeg.coerceIn(-LOW_SPEED_MAX_LEAN_DEG, LOW_SPEED_MAX_LEAN_DEG)
            return Result(clamped, LeanConfidence.LOW_SPEED_UNRELIABLE)
        }

        // yaw 데이터 없음 → 검증 불가, 원시 보존(fail-open).
        if (yawRateRadPerSec.isNaN()) return Result(rawRollDeg, LeanConfidence.VALID)

        val vMps = speedKmh / KMH_PER_MPS
        val expectedRad = atan((vMps * abs(yawRateRadPerSec) / GRAVITY).toDouble())
        val expectedMagDeg = Math.toDegrees(expectedRad).toFloat()

        // 측정 크기가 예측치를 허용 오차 이상 '초과'하면 노이즈로 보고 예측치로 클리핑(과대 보고 억제).
        // 측정이 예측보다 작은 경우는 폰 조작 오염 시나리오가 아니므로 보존한다.
        val excess = abs(rawRollDeg) - expectedMagDeg
        return if (excess > toleranceFor(speedKmh)) {
            Result(sign(rawRollDeg) * expectedMagDeg, LeanConfidence.OUTLIER_NOISE)
        } else {
            Result(rawRollDeg, LeanConfidence.VALID)
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

    /** 극저속 구간 보정 뱅킹각 상한(도). 정차 시 정적 lean 상한이자 폰 조작 노이즈 캡. */
    const val LOW_SPEED_MAX_LEAN_DEG = 15f

    private const val GRAVITY = 9.81f
    private const val KMH_PER_MPS = 3.6f
}
