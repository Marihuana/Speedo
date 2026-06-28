package kr.yooreka.speedo.data.sensor.datasource

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sqrt

/**
 * GPS 신호 튐 방지 가드(F-01a, PRD §4.1).
 *
 * 수신된 GPS 좌표/속도가 물리적으로 불가능하면 이상치(Outlier)로 판정해 폐기한다. 폐기된 좌표는
 * "미수신(Drop)"으로 취급하므로, 소비처는 직전 신뢰 값을 유지하고 이후 정상 좌표가 들어오면
 * 시간 비례 선형 보간(F-13c/F-13d)으로 구간을 메운다.
 *
 * 판정 규칙:
 * 1. **절대 속도 상한([maxSpeedKmh], 기본 350km/h)**: 초과 시 즉시 폐기(센서 오류/신호 튐).
 * 2. **도약 거리 필터**: 직전 신뢰 좌표로부터의 실제 이동거리가 예상 이동거리(직전 속도×Δt)의
 *    [jumpFactor] 배를 넘고 동시에 [minJumpMeters] 도 초과하면 폐기.
 * 3. **방향 역전 필터**: 직전 이동 벡터와 현재 이동 벡터의 사잇각 코사인이 [reversalCosThreshold]
 *    (기본 cos120°=-0.5) 미만이면(급격한 역주행) 폐기. 단, 구간 이동이 [directionMinSegmentMeters]
 *    미만이면 방향이 노이즈에 민감하므로 검사하지 않는다.
 *
 * 연속 폐기가 [maxConsecutiveRejects] 회에 도달하면(장기 음영/실제 큰 이동 등) 기준점을 강제
 * 재동기화하여 영구 고착을 방지한다.
 *
 * 안드로이드 프레임워크에 의존하지 않도록 구간 거리 계산([distanceMeters])을 주입받아 순수
 * 단위 테스트가 가능하다.
 */
class GpsSignalGuard(
    private val maxSpeedKmh: Float = MAX_SPEED_KMH,
    private val jumpFactor: Float = JUMP_FACTOR,
    private val minJumpMeters: Float = MIN_JUMP_METERS,
    private val reversalCosThreshold: Float = REVERSAL_COS_THRESHOLD,
    private val directionMinSegmentMeters: Float = DIRECTION_MIN_SEGMENT_METERS,
    private val maxConsecutiveRejects: Int = MAX_CONSECUTIVE_REJECTS,
    private val distanceMeters: (lat1: Double, lng1: Double, lat2: Double, lng2: Double) -> Float,
) {
    private var lastLat: Double? = null
    private var lastLng: Double? = null
    private var lastTimeMs: Long = 0L
    private var lastSpeedKmh: Float = 0f

    // 직전 이동 벡터(경도는 위도 cos 보정). 방향 역전 판정에만 사용한다.
    private var headingX: Double = 0.0
    private var headingY: Double = 0.0
    private var hasHeading: Boolean = false

    private var consecutiveRejects: Int = 0

    /** 새 주행/세션 시작 시 호출하여 기준점을 초기화한다. */
    fun reset() {
        lastLat = null
        lastLng = null
        lastTimeMs = 0L
        lastSpeedKmh = 0f
        headingX = 0.0
        headingY = 0.0
        hasHeading = false
        consecutiveRejects = 0
    }

    /**
     * 새 GPS fix 를 평가한다.
     * @return true 면 신뢰 좌표(채택), false 면 이상치(폐기 — 소비처는 직전 값 유지).
     */
    fun accept(
        lat: Double,
        lng: Double,
        speedKmh: Float,
        timeMs: Long,
    ): Boolean {
        // 1) 절대 속도 상한 초과 → 폐기.
        if (speedKmh > maxSpeedKmh) return onReject(lat, lng, speedKmh, timeMs)

        val prevLat = lastLat
        val prevLng = lastLng
        if (prevLat == null || prevLng == null) {
            // 첫 좌표: 기준점만 설정(방향 벡터 없음).
            acceptPoint(prevLat, prevLng, lat, lng, speedKmh, timeMs)
            return true
        }

        val moved = distanceMeters(prevLat, prevLng, lat, lng)
        // prev 좌표가 있으면 lastTimeMs 는 항상 설정돼 있다. dt<=0(시간 역전/동시)이면 도약 판정을 건너뛴다.
        val dtSec = (timeMs - lastTimeMs) / MILLIS_PER_SEC

        // 2) 도약 거리 필터: 직전 속도로 갈 수 있는 거리의 jumpFactor 배 + 최소 임계 동시 초과.
        if (dtSec > 0.0) {
            val expectedMeters = (lastSpeedKmh / KMH_PER_MPS) * dtSec
            val limit = max(expectedMeters * jumpFactor, minJumpMeters.toDouble())
            if (moved > limit) return onReject(lat, lng, speedKmh, timeMs)
        }

        // 3) 방향 역전 필터: 충분히 이동했을 때만(작은 이동은 방향 노이즈가 크다).
        if (hasHeading && moved >= directionMinSegmentMeters) {
            val (curX, curY) = movementVector(prevLat, prevLng, lat, lng)
            if (cosBetween(headingX, headingY, curX, curY) < reversalCosThreshold) {
                return onReject(lat, lng, speedKmh, timeMs)
            }
        }

        acceptPoint(prevLat, prevLng, lat, lng, speedKmh, timeMs)
        return true
    }

    /** 좌표 채택: 기준점/속도/진행 방향을 갱신하고 연속 폐기 카운터를 리셋한다. */
    private fun acceptPoint(
        prevLat: Double?,
        prevLng: Double?,
        lat: Double,
        lng: Double,
        speedKmh: Float,
        timeMs: Long,
    ) {
        if (prevLat != null && prevLng != null) {
            val (dx, dy) = movementVector(prevLat, prevLng, lat, lng)
            if (dx != 0.0 || dy != 0.0) {
                headingX = dx
                headingY = dy
                hasHeading = true
            }
        }
        lastLat = lat
        lastLng = lng
        lastSpeedKmh = speedKmh
        lastTimeMs = timeMs
        consecutiveRejects = 0
    }

    /**
     * 이상치 처리: 기준점을 유지(폐기)하되, 연속 폐기가 임계에 도달하면 기준점을 강제 재동기화하여
     * (장기 음영 후 실제 큰 이동 등) 영구 고착을 막는다. 재동기화 시에는 채택으로 본다.
     */
    private fun onReject(
        lat: Double,
        lng: Double,
        speedKmh: Float,
        timeMs: Long,
    ): Boolean {
        consecutiveRejects++
        // 속도 상한 초과는 명백한 센서 오류이므로 재동기화 대상에서 제외(항상 폐기).
        if (speedKmh <= maxSpeedKmh && consecutiveRejects >= maxConsecutiveRejects) {
            // 방향 정보는 신뢰할 수 없으므로 버리고 기준점만 다시 잡는다.
            hasHeading = false
            lastLat = lat
            lastLng = lng
            lastSpeedKmh = speedKmh
            lastTimeMs = timeMs
            consecutiveRejects = 0
            return true
        }
        return false
    }

    /** 두 좌표 간 이동 벡터(경도는 위도 cos 보정). 방향 비교용이라 단위는 무차원. */
    private fun movementVector(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Pair<Double, Double> {
        val midLatRad = Math.toRadians((lat1 + lat2) / 2.0)
        val dx = (lng2 - lng1) * cos(midLatRad)
        val dy = lat2 - lat1
        return dx to dy
    }

    /** 두 벡터 사잇각의 코사인. 한쪽이 영벡터면 1(역전 아님)로 본다. */
    private fun cosBetween(
        ax: Double,
        ay: Double,
        bx: Double,
        by: Double,
    ): Float {
        val magA = sqrt(ax * ax + ay * ay)
        val magB = sqrt(bx * bx + by * by)
        if (magA == 0.0 || magB == 0.0) return 1f
        return ((ax * bx + ay * by) / (magA * magB)).toFloat()
    }

    companion object {
        /** 절대 속도 한계(km/h). 초고성능/서킷 주행을 감안한 PRD §4.1 기준값. */
        const val MAX_SPEED_KMH = 350f

        /** 예상 이동거리 대비 허용 배수(도약 판정). */
        const val JUMP_FACTOR = 3f

        /** 도약 판정 최소 임계(m). 저속/짧은 Δt 에서의 오탐을 막는 하한. */
        const val MIN_JUMP_METERS = 50f

        /** 방향 역전 판정 코사인 임계(기본 cos120° = -0.5). 이보다 작으면 역주행으로 본다. */
        const val REVERSAL_COS_THRESHOLD = -0.5f

        /** 방향 검사를 적용할 최소 구간 이동(m). 미만이면 방향 노이즈가 커 검사하지 않는다. */
        const val DIRECTION_MIN_SEGMENT_METERS = 5f

        /** 연속 폐기 임계. 도달 시 기준점을 강제 재동기화한다. */
        const val MAX_CONSECUTIVE_REJECTS = 5

        private const val KMH_PER_MPS = 3.6
        private const val MILLIS_PER_SEC = 1000.0
    }
}
