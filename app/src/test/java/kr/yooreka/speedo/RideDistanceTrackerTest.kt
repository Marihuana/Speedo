package kr.yooreka.speedo

import kr.yooreka.speedo.data.sensor.repository.RideDistanceTracker
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * 테스트용 평면 근사 거리 함수(미터).
 * 위도 1도 ≈ 111,320m, 경도 1도 ≈ 111,320m * cos(위도).
 * Location.distanceBetween 대신 결정적 값을 제공해 누적/게이트 로직만 검증한다.
 */
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

/** 게이트 없이(기본) 평면 거리 함수를 쓰는 트래커. */
private fun tracker(
    minMeters: Float = 0f,
    maxAccuracy: Float = 0f,
) = RideDistanceTracker(minMeters, maxAccuracy, ::planarMeters)

class RideDistanceTrackerTest {
    // ---------- 기본 누적 로직 (게이트 비활성) ----------

    @Test
    fun `initial distance is zero`() {
        val t = tracker()
        assertEquals(0f, t.totalMeters, 0f)
        assertEquals(0f, t.totalKm, 0f)
    }

    @Test
    fun `first valid point sets baseline without adding distance`() {
        val t = tracker()
        t.add(37.5665, 126.9780) // 서울시청
        assertEquals(0f, t.totalMeters, 0f)
    }

    @Test
    fun `accumulates distance across consecutive points`() {
        val t = tracker()
        t.add(37.0000, 127.0000)
        t.add(37.0010, 127.0000)
        t.add(37.0020, 127.0000)
        // 두 구간 합 ≈ 222.6m
        assertEquals(222.64f, t.totalMeters, 0.5f)
    }

    @Test
    fun `ignores invalid zero coordinates`() {
        val t = tracker()
        t.add(37.0000, 127.0000)
        t.add(0.0, 0.0) // GPS 미수신 — 무시
        t.add(37.0010, 127.0000) // 기준점은 여전히 (37.0000, 127.0000)
        assertEquals(111.32f, t.totalMeters, 0.5f)
    }

    @Test
    fun `reset clears accumulated distance and baseline`() {
        val t = tracker()
        t.add(37.0000, 127.0000)
        t.add(37.0010, 127.0000)
        assertEquals(111.32f, t.totalMeters, 0.5f)

        t.reset()
        assertEquals(0f, t.totalMeters, 0f)

        t.add(37.0010, 127.0000) // reset 후 첫 좌표는 기준점만
        assertEquals(0f, t.totalMeters, 0f)
    }

    @Test
    fun `totalKm converts meters correctly`() {
        val t = RideDistanceTracker(distanceMeters = { _, _, _, _ -> 2500f })
        t.add(37.0, 127.0)
        t.add(37.1, 127.0) // +2500m
        assertEquals(2.5f, t.totalKm, 0.001f)
    }

    // ---------- 최소 구간거리 게이트 ----------

    @Test
    fun `min distance gate ignores sub-threshold jitter and keeps baseline`() {
        // 임계값 5m. 0.00002도 ≈ 2.2m 짜리 미세 흔들림을 반복.
        val t = tracker(minMeters = 5f)
        t.add(37.000000, 127.000000)
        t.add(37.000020, 127.000000) // ≈2.2m < 5m → 무시, 기준점 유지
        t.add(36.999980, 127.000000) // 기준점(37.000000) 대비 ≈2.2m < 5m → 무시
        assertEquals("정차 중 지터는 거리에 더해지지 않아야 한다", 0f, t.totalMeters, 0.01f)
    }

    @Test
    fun `min distance gate accumulates once displacement exceeds threshold`() {
        // 임계값 5m. 기준점 대비 변위가 누적되어 임계값을 넘으면 그때 반영된다.
        val t = tracker(minMeters = 5f)
        t.add(37.000000, 127.000000)
        t.add(37.000020, 127.000000) // ≈2.2m → 무시(기준점 유지)
        t.add(37.000060, 127.000000) // 기준점 대비 ≈6.7m ≥ 5m → 반영
        assertEquals(6.68f, t.totalMeters, 0.5f)
    }

    // ---------- 정확도 게이트 ----------

    @Test
    fun `accuracy gate rejects poor fixes`() {
        val t = tracker(maxAccuracy = 25f)
        t.add(37.0000, 127.0000, accuracyMeters = 5f) // 양호 → 기준점
        t.add(37.0010, 127.0000, accuracyMeters = 50f) // 부정확(>25) → 무시, 기준점 유지
        t.add(37.0020, 127.0000, accuracyMeters = 8f) // 양호 → 기준점(37.0000) 대비 ≈222.6m
        assertEquals(222.64f, t.totalMeters, 0.5f)
    }

    @Test
    fun `accuracy zero is treated as unknown and accepted`() {
        val t = tracker(maxAccuracy = 25f)
        t.add(37.0000, 127.0000, accuracyMeters = 0f) // 알 수 없음 → 통과
        t.add(37.0010, 127.0000, accuracyMeters = 0f)
        assertEquals(111.32f, t.totalMeters, 0.5f)
    }
}
