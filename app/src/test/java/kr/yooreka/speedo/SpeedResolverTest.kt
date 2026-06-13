package kr.yooreka.speedo

import kr.yooreka.speedo.data.sensor.datasource.SpeedResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedResolverTest {

    @Test
    fun `no speed returns zero`() {
        assertEquals(0f, SpeedResolver.toKmh(hasSpeed = false, speedMps = 5f, hasSpeedAccuracy = true, speedAccuracyMps = 1f), 0f)
    }

    @Test
    fun `reliable moving speed converts to kmh`() {
        // 10 m/s, 정확도 1 m/s → 신뢰 → 36 km/h
        assertEquals(36f, SpeedResolver.toKmh(hasSpeed = true, speedMps = 10f, hasSpeedAccuracy = true, speedAccuracyMps = 1f), 0.001f)
    }

    @Test
    fun `stationary jitter suppressed when speed within accuracy`() {
        // 정지 중 가짜 3 m/s(≈10.8km/h)지만 속도 불확실성도 3 m/s → 0 처리
        assertEquals(0f, SpeedResolver.toKmh(hasSpeed = true, speedMps = 3f, hasSpeedAccuracy = true, speedAccuracyMps = 3f), 0f)
    }

    @Test
    fun `low speed deadband when accuracy unavailable`() {
        // 정확도 미제공 + 0.5 m/s(<0.7) → 0
        assertEquals(0f, SpeedResolver.toKmh(hasSpeed = true, speedMps = 0.5f, hasSpeedAccuracy = false, speedAccuracyMps = 0f), 0f)
    }

    @Test
    fun `above deadband without accuracy passes through`() {
        // 정확도 미제공 + 5 m/s → 18 km/h
        assertEquals(18f, SpeedResolver.toKmh(hasSpeed = true, speedMps = 5f, hasSpeedAccuracy = false, speedAccuracyMps = 0f), 0.001f)
    }

    @Test
    fun `speed clearly above accuracy is trusted`() {
        // 8 m/s, 정확도 2 m/s → 8 > 2 → 28.8 km/h
        assertEquals(28.8f, SpeedResolver.toKmh(hasSpeed = true, speedMps = 8f, hasSpeedAccuracy = true, speedAccuracyMps = 2f), 0.001f)
    }
}
