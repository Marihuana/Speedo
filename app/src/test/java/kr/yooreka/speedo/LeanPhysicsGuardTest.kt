package kr.yooreka.speedo

import kr.yooreka.speedo.domain.model.LeanConfidence
import kr.yooreka.speedo.domain.model.LeanPhysicsGuard
import org.junit.Assert.assertEquals
import org.junit.Test

class LeanPhysicsGuardTest {
    @Test
    fun `nan yaw rate at speed preserves raw roll as valid (fail-open)`() {
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 35f, speedKmh = 60f, yawRateRadPerSec = Float.NaN)
        assertEquals(LeanConfidence.VALID, r.confidence)
        assertEquals(35f, r.roll, 0.001f)
    }

    @Test
    fun `low speed within 15deg keeps raw and marks unreliable`() {
        // 3km/h, 10° (≤15) → 원시 보존, LOW_SPEED_UNRELIABLE
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 10f, speedKmh = 3f, yawRateRadPerSec = Float.NaN)
        assertEquals(LeanConfidence.LOW_SPEED_UNRELIABLE, r.confidence)
        assertEquals(10f, r.roll, 0.001f)
    }

    @Test
    fun `low speed over 15deg is clamped to 15`() {
        // 정차(3km/h)에서 폰 조작 42° → 15°로 클램프, LOW_SPEED_UNRELIABLE
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 42f, speedKmh = 3f, yawRateRadPerSec = 0.5f)
        assertEquals(LeanConfidence.LOW_SPEED_UNRELIABLE, r.confidence)
        assertEquals(15f, r.roll, 0.001f)
    }

    @Test
    fun `low speed clamp preserves sign`() {
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = -40f, speedKmh = 0f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.LOW_SPEED_UNRELIABLE, r.confidence)
        assertEquals(-15f, r.roll, 0.001f)
    }

    @Test
    fun `roll consistent with yaw model is valid`() {
        // v=10m/s(36km/h), ω=0.173 → 예측≈10°. 측정 12° → 오차 2° < 허용 12° → VALID(원시 유지)
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 12f, speedKmh = 36f, yawRateRadPerSec = 0.173f)
        assertEquals(LeanConfidence.VALID, r.confidence)
        assertEquals(12f, r.roll, 0.001f)
    }

    @Test
    fun `excessive roll at near-zero yaw is clipped as outlier`() {
        // 20km/h(허용 15°), yaw≈0 → 예측≈0°. 측정 40° → 과대 → OUTLIER, 예측치(≈0)로 클리핑
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 40f, speedKmh = 20f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.OUTLIER_NOISE, r.confidence)
        assertEquals(0f, r.roll, 0.1f)
    }

    @Test
    fun `outlier clipping preserves measured sign`() {
        // v=10m/s, ω=0.173 → 예측≈10°. 측정 -40° → OUTLIER, 부호 보존하여 -10°로 클리핑
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = -40f, speedKmh = 36f, yawRateRadPerSec = 0.173f)
        assertEquals(LeanConfidence.OUTLIER_NOISE, r.confidence)
        assertEquals(-10f, r.roll, 0.2f)
    }

    @Test
    fun `roll below expected is not fabricated upward`() {
        // v=10m/s, ω=0.566 → 예측≈30°. 측정 5°(과소) → 오염 시나리오 아님 → VALID, 원시 5° 유지
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 5f, speedKmh = 36f, yawRateRadPerSec = 0.566f)
        assertEquals(LeanConfidence.VALID, r.confidence)
        assertEquals(5f, r.roll, 0.001f)
    }

    @Test
    fun `tolerance widens at low speed band`() {
        // yaw≈0 → 예측≈0°. 측정 18°.
        // 10km/h 구간(허용 20°): 오차 18° < 20° → VALID
        val low = LeanPhysicsGuard.evaluate(rawRollDeg = 18f, speedKmh = 10f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.VALID, low.confidence)
        // 35km/h 구간(허용 12°): 오차 18° > 12° → OUTLIER
        val high = LeanPhysicsGuard.evaluate(rawRollDeg = 18f, speedKmh = 35f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.OUTLIER_NOISE, high.confidence)
    }
}
