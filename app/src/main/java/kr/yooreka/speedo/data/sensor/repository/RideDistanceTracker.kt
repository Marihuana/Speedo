package kr.yooreka.speedo.data.sensor.repository

/**
 * 주행 중 누적 이동 거리를 계산한다.
 *
 * 구간 거리 계산 함수([distanceMeters])를 주입받아 안드로이드 프레임워크
 * (`Location.distanceBetween`)에 의존하지 않으므로 순수 단위 테스트가 가능하다.
 *
 * GPS 노이즈로 인한 거리 과대계상을 막기 위해 두 가지 게이트를 둔다:
 * - **정확도 게이트([maxAccuracyMeters])**: 정확도가 이 값보다 나쁜(큰) 측위는 통째로 무시한다.
 * - **최소 구간거리 게이트([minDistanceMeters])**: 직전 채택 좌표와의 구간 거리가 이 값보다
 *   작으면(정차 중 미세 흔들림 등) 거리에 더하지 않고 **기준점도 그대로 유지**한다.
 *   덕분에 천천히 이동해도 변위가 임계값을 넘는 순간 한 번에 반영된다.
 *
 * 두 값 모두 기본값 0이면 해당 게이트는 비활성화된다.
 * 그 밖에:
 * - `(0.0, 0.0)` 좌표(GPS 미수신)는 무시한다.
 * - 첫 유효 좌표는 기준점만 설정하고 거리에는 더하지 않는다.
 */
class RideDistanceTracker(
    private val minDistanceMeters: Float = 0f,
    private val maxAccuracyMeters: Float = 0f,
    private val distanceMeters: (lat1: Double, lng1: Double, lat2: Double, lng2: Double) -> Float
) {
    private var lastLat: Double? = null
    private var lastLng: Double? = null

    @Volatile
    var totalMeters: Float = 0f
        private set

    val totalKm: Float
        get() = totalMeters / 1000f

    /** 새 주행 시작 시 호출하여 누적값을 초기화한다. */
    fun reset() {
        lastLat = null
        lastLng = null
        totalMeters = 0f
    }

    /**
     * 새 위치를 반영한다.
     * @param accuracyMeters 수평 정확도(미터). 0이면 알 수 없음으로 보고 통과시킨다.
     */
    fun add(lat: Double, lng: Double, accuracyMeters: Float = 0f) {
        // 1) 무효 좌표(GPS 미수신) 무시
        if (lat == 0.0 && lng == 0.0) return

        // 2) 정확도 게이트: 정확도를 알 수 있고(>0) 임계값보다 나쁘면 무시
        if (maxAccuracyMeters > 0f && accuracyMeters > maxAccuracyMeters) return

        val prevLat = lastLat
        val prevLng = lastLng
        if (prevLat != null && prevLng != null) {
            val segment = distanceMeters(prevLat, prevLng, lat, lng)
            // 3) 최소 구간거리 게이트: 너무 작은 이동은 무시하고 기준점 유지
            if (segment < minDistanceMeters) return
            totalMeters += segment
        }
        lastLat = lat
        lastLng = lng
    }
}
