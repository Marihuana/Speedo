package kr.yooreka.speedo

import kr.yooreka.speedo.domain.model.LeanConfidence
import kr.yooreka.speedo.domain.model.LeanPhysicsGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeanPhysicsGuardTest {
    @Test
    fun `nan yaw rate preserves raw roll as reliable (fail-open)`() {
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 35f, speedKmh = 60f, yawRateRadPerSec = Float.NaN)
        assertEquals(LeanConfidence.RELIABLE, r.confidence)
        assertEquals(35f, r.roll, 0.001f)
        assertTrue(r.includeInMax)
    }

    @Test
    fun `stationary low speed without yaw is excluded from max`() {
        // 극저속(3km/h) + yaw 없음(정지/폰 조작) → LOW_SPEED_UNRELIABLE, 최대각 집계 제외
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 42f, speedKmh = 3f, yawRateRadPerSec = Float.NaN)
        assertEquals(LeanConfidence.LOW_SPEED_UNRELIABLE, r.confidence)
        assertEquals(42f, r.roll, 0.001f)
        assertFalse(r.includeInMax)
    }

    @Test
    fun `low speed but turning preserves raw roll and counts toward max`() {
        // 3km/h < 5km/h 이지만 yaw 0.5rad/s > 임계 → 저속 선회(발 버팀 U턴) → 원시 보존 + 집계 포함
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 25f, speedKmh = 3f, yawRateRadPerSec = 0.5f)
        assertEquals(LeanConfidence.LOW_SPEED_UNRELIABLE, r.confidence)
        assertEquals(25f, r.roll, 0.001f)
        assertTrue(r.includeInMax)
    }

    @Test
    fun `low speed with negligible yaw is excluded from max`() {
        // 3km/h + yaw 0.05rad/s < 임계(0.15) → 선회 아님(정지 흔들림) → 집계 제외
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 30f, speedKmh = 3f, yawRateRadPerSec = 0.05f)
        assertEquals(LeanConfidence.LOW_SPEED_UNRELIABLE, r.confidence)
        assertFalse(r.includeInMax)
    }

    @Test
    fun `roll consistent with yaw model is reliable`() {
        // v=10m/s(36km/h), ω=0.173 → 예측≈10°. 측정 12° → 오차 2° < 허용 12° → RELIABLE(원시 유지)
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 12f, speedKmh = 36f, yawRateRadPerSec = 0.173f)
        assertEquals(LeanConfidence.RELIABLE, r.confidence)
        assertEquals(12f, r.roll, 0.001f)
        assertTrue(r.includeInMax)
    }

    @Test
    fun `excessive roll at near-zero yaw is clipped as outlier and excluded from max`() {
        // 20km/h(허용 15°), yaw≈0 → 예측≈0°. 측정 40° → 과대 → OUTLIER, 예측치(≈0)로 클리핑, 집계 제외
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 40f, speedKmh = 20f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.OUTLIER_NOISE, r.confidence)
        assertEquals(0f, r.roll, 0.1f)
        assertFalse(r.includeInMax)
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
        // v=10m/s, ω=0.566 → 예측≈30°. 측정 5°(과소) → 오염 시나리오 아님 → RELIABLE, 원시 5° 유지
        val r = LeanPhysicsGuard.evaluate(rawRollDeg = 5f, speedKmh = 36f, yawRateRadPerSec = 0.566f)
        assertEquals(LeanConfidence.RELIABLE, r.confidence)
        assertEquals(5f, r.roll, 0.001f)
    }

    @Test
    fun `tolerance widens at low speed band`() {
        // yaw≈0 → 예측≈0°. 측정 18°.
        // 10km/h 구간(허용 20°): 오차 18° < 20° → RELIABLE
        val low = LeanPhysicsGuard.evaluate(rawRollDeg = 18f, speedKmh = 10f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.RELIABLE, low.confidence)
        // 35km/h 구간(허용 12°): 오차 18° > 12° → OUTLIER
        val high = LeanPhysicsGuard.evaluate(rawRollDeg = 18f, speedKmh = 35f, yawRateRadPerSec = 0f)
        assertEquals(LeanConfidence.OUTLIER_NOISE, high.confidence)
    }
}
