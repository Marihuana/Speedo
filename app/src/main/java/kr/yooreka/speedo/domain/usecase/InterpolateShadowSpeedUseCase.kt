package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.RideTelemetry
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GPS 음영 구간(터널 등) 속도 보간(F-13d).
 *
 * 터널처럼 장시간 GPS 를 받지 못하면 해당 구간 행의 속도가 0/정체로 남아 경로는 그려져도(F-13c)
 * 속도가 표기되지 않는다. 이를 보완하기 위해 음영 **진입 직전 앵커**와 **진출 직후 앵커**의
 * 좌표·시간으로 평균속도를 역산(`거리 ÷ 경과시간`)하여 그 사이 행 속도에 채운다.
 *
 * - 앵커 = 좌표(lat/lng) non-null 행. 앵커가 2개 미만이면 그대로 반환.
 * - 인접 앵커 간 간격이 [SHADOW_GAP_MS] 미만(정상 GPS 수신)이면 손대지 않는다(실측 속도 보존).
 * - 앵커 자체 속도는 보존하고, 사이의 내부(음영) 행 속도만 평균속도로 대체한다.
 * - F-13c(좌표 보간) **이전에** 적용해야 한다(좌표 보간 후엔 모든 행이 앵커처럼 보임).
 */
class InterpolateShadowSpeedUseCase
    @Inject
    constructor() {
        operator fun invoke(points: List<RideTelemetry>): List<RideTelemetry> {
            val anchorIndices = points.indices.filter { points[it].hasLocation() }
            if (anchorIndices.size < 2) return points

            val result = points.toMutableList()
            for (k in 0 until anchorIndices.size - 1) {
                val startIdx = anchorIndices[k]
                val endIdx = anchorIndices[k + 1]
                val start = points[startIdx]
                val end = points[endIdx]

                val elapsedMs = end.timestamp - start.timestamp
                // 정상 GPS 수신 구간은 실측 속도를 그대로 둔다. 음영(장시간 미수신)만 역산.
                if (elapsedMs < SHADOW_GAP_MS || endIdx - startIdx < 2) continue

                val distanceMeters = haversineMeters(start, end)
                val avgKmh = (distanceMeters / (elapsedMs / MILLIS_PER_SEC)).toFloat() * MS_TO_KMH

                for (i in (startIdx + 1) until endIdx) {
                    result[i] = points[i].copy(speed = avgKmh)
                }
            }
            return result
        }

        private fun RideTelemetry.hasLocation(): Boolean = latitude != null && longitude != null

        /** 두 좌표 간 대권(great-circle) 거리(m). 외부 SDK 의존 없이 순수 계산(Haversine). */
        private fun haversineMeters(
            a: RideTelemetry,
            b: RideTelemetry,
        ): Double {
            val lat1 = Math.toRadians(a.latitude!!)
            val lat2 = Math.toRadians(b.latitude!!)
            val dLat = lat2 - lat1
            val dLng = Math.toRadians(b.longitude!! - a.longitude!!)
            val h = sin(dLat / 2).pow2() + cos(lat1) * cos(lat2) * sin(dLng / 2).pow2()
            return 2.0 * EARTH_RADIUS_M * asin(sqrt(h))
        }

        private fun Double.pow2(): Double = this * this

        companion object {
            /** 인접 GPS 앵커 간격이 이보다 크면 음영 구간으로 본다(정상 GPS 는 약 1Hz). */
            private const val SHADOW_GAP_MS = 3000L
            private const val MILLIS_PER_SEC = 1000.0
            private const val MS_TO_KMH = 3.6f
            private const val EARTH_RADIUS_M = 6_371_000.0
        }
    }
