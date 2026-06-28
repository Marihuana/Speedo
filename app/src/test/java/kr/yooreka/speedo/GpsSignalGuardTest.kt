package kr.yooreka.speedo

import kr.yooreka.speedo.data.sensor.datasource.GpsSignalGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sqrt

/** 결정적 평면 근사 거리(미터). RideDistanceTrackerTest 와 동일한 근사. */
private fun planarMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Float {
    val mPerDeg = 111_320.0
    val dLat = (lat2 - lat1) * mPerDeg
    val dLng = (lng2 - lng1) * mPerDeg * cos(Math.toRadians((lat1 + lat2) / 2))
    return sqrt(dLat * dLat + dLng * dLng).toFloat()
}

private fun guard() = GpsSignalGuard(distanceMeters = ::planarMeters)

class GpsSignalGuardTest {
    @Test
    fun `first fix is always accepted as baseline`() {
        val g = guard()
        assertTrue(g.accept(37.0000, 127.0000, speedKmh = 0f, timeMs = 0L))
    }

    @Test
    fun `speed over absolute limit is rejected`() {
        val g = guard()
        g.accept(37.0000, 127.0000, speedKmh = 60f, timeMs = 0L)
        // 351km/h > 350 상한 → 폐기
        assertFalse(g.accept(37.0001, 127.0000, speedKmh = 351f, timeMs = 1000L))
    }

    @Test
    fun `realistic forward movement is accepted`() {
        val g = guard()
        g.accept(37.0000, 127.0000, speedKmh = 72f, timeMs = 0L) // 20 m/s
        // 1초 후 ≈20m 북진. 예상≈20m, 한계=max(60,50)=60m → 채택
        assertTrue(g.accept(37.000180, 127.0000, speedKmh = 72f, timeMs = 1000L))
    }

    @Test
    fun `implausible jump beyond expected distance is rejected`() {
        val g = guard()
        g.accept(37.0000, 127.0000, speedKmh = 36f, timeMs = 0L) // 10 m/s
        // 1초 만에 ≈1.1km 도약. 예상=10m, 한계=max(30,50)=50m → 폐기
        assertFalse(g.accept(37.0100, 127.0000, speedKmh = 36f, timeMs = 1000L))
    }

    @Test
    fun `small jump within min threshold is accepted even at zero prior speed`() {
        val g = guard()
        g.accept(37.0000, 127.0000, speedKmh = 0f, timeMs = 0L)
        // 예상=0m 이지만 minJumpMeters(50m) 하한 덕분에 ≈22m 이동은 채택
        assertTrue(g.accept(37.0002, 127.0000, speedKmh = 5f, timeMs = 1000L))
    }

    @Test
    fun `sharp direction reversal is rejected`() {
        val g = guard()
        g.accept(37.0000, 127.0000, speedKmh = 36f, timeMs = 0L)
        // 북진하여 진행 방향 확립(≈33m, 예상≈30m? 한계 50m 이내라 채택)
        assertTrue(g.accept(37.0003, 127.0000, speedKmh = 36f, timeMs = 1000L))
        // 다음 fix 가 급격히 남쪽(역주행, 사잇각≈180°) ≈33m → 방향 역전 폐기
        assertFalse(g.accept(37.0000, 127.0000, speedKmh = 36f, timeMs = 2000L))
    }

    @Test
    fun `continuing same direction is accepted`() {
        val g = guard()
        g.accept(37.0000, 127.0000, speedKmh = 36f, timeMs = 0L)
        assertTrue(g.accept(37.0003, 127.0000, speedKmh = 36f, timeMs = 1000L))
        // 계속 북진 → 방향 일치, 채택
        assertTrue(g.accept(37.0006, 127.0000, speedKmh = 36f, timeMs = 2000L))
    }

    @Test
    fun `baseline resyncs after consecutive rejects`() {
        val g = GpsSignalGuard(maxConsecutiveRejects = 3, distanceMeters = ::planarMeters)
        g.accept(37.0000, 127.0000, speedKmh = 36f, timeMs = 0L)
        // 비속도성(거리 도약) 연속 폐기 3회째에 기준점 강제 재동기화 → 채택
        assertFalse(g.accept(37.0100, 127.0000, speedKmh = 36f, timeMs = 1000L))
        assertFalse(g.accept(37.0200, 127.0000, speedKmh = 36f, timeMs = 2000L))
        assertTrue(g.accept(37.0300, 127.0000, speedKmh = 36f, timeMs = 3000L))
    }

    @Test
    fun `speed limit violation never resyncs`() {
        val g = GpsSignalGuard(maxConsecutiveRejects = 2, distanceMeters = ::planarMeters)
        g.accept(37.0000, 127.0000, speedKmh = 36f, timeMs = 0L)
        // 속도 상한 초과는 명백한 센서 오류 → 연속이어도 재동기화하지 않고 항상 폐기
        assertFalse(g.accept(37.0001, 127.0000, speedKmh = 999f, timeMs = 1000L))
        assertFalse(g.accept(37.0002, 127.0000, speedKmh = 999f, timeMs = 2000L))
        assertFalse(g.accept(37.0003, 127.0000, speedKmh = 999f, timeMs = 3000L))
    }
}
