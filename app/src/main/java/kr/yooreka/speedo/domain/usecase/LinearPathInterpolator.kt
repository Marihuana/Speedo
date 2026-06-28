package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.RideTelemetry
import javax.inject.Inject

/**
 * 좌표 없는 시간주기 행을 인접 GPS 앵커의 타임스탬프 비례로 선형 보간한다(F-13c 기본 전략).
 *
 * - 앵커 = 좌표(lat/lng)가 모두 non-null 인 행.
 * - 앵커 0개: 입력 그대로 반환(그릴 경로 없음).
 * - 앵커 1개: 모든 null 행을 그 앵커 좌표로 채움.
 * - 인접 앵커 A(tA), B(tB) 사이의 null 행: f = (t-tA)/(tB-tA), lat = latA + (latB-latA)*f (lng 동일식).
 *   tB == tA 이면 분모 0을 피해 A 좌표로 채움.
 * - 첫 앵커 이전 / 마지막 앵커 이후의 null 행: 가장 가까운 앵커 좌표로 클램프(외삽 금지).
 *
 * 좌표만 채우고 speed/roll/brake 등은 [RideTelemetry.copy] 로 보존한다.
 */
class LinearPathInterpolator
    @Inject
    constructor() : PathInterpolator {
        override fun interpolate(points: List<RideTelemetry>): List<RideTelemetry> {
            if (points.isEmpty()) return points

            val anchorIndices = points.indices.filter { points[it].hasLocation() }
            if (anchorIndices.isEmpty()) return points

            // 앵커가 1개뿐이면 모든 행을 그 좌표로 채운다.
            if (anchorIndices.size == 1) {
                val anchor = points[anchorIndices.first()]
                return points.map { it.withLocation(anchor.latitude, anchor.longitude) }
            }

            val firstAnchor = points[anchorIndices.first()]
            val lastAnchor = points[anchorIndices.last()]
            // 다음 앵커를 가리키는 포인터로 인접 앵커 구간을 한 번에 훑는다(O(n)).
            var nextAnchorPos = 0

            return points.mapIndexed { index, point ->
                when {
                    point.hasLocation() -> point
                    // 첫 앵커 이전: 가장 가까운(첫) 앵커로 클램프.
                    index < anchorIndices.first() -> point.withLocation(firstAnchor.latitude, firstAnchor.longitude)
                    // 마지막 앵커 이후: 가장 가까운(마지막) 앵커로 클램프.
                    index > anchorIndices.last() -> point.withLocation(lastAnchor.latitude, lastAnchor.longitude)
                    else -> {
                        while (anchorIndices[nextAnchorPos] <= index) nextAnchorPos++
                        val before = points[anchorIndices[nextAnchorPos - 1]]
                        val after = points[anchorIndices[nextAnchorPos]]
                        point.withLocation(lerp(before, after, point.timestamp))
                    }
                }
            }
        }

        /** 앵커 [before]→[after] 사이에서 [timestamp] 비례로 보간한 좌표. 분모 0이면 [before] 좌표. */
        private fun lerp(
            before: RideTelemetry,
            after: RideTelemetry,
            timestamp: Long,
        ): LatLng {
            val span = after.timestamp - before.timestamp
            if (span <= 0L) return LatLng(before.latitude!!, before.longitude!!)
            val fraction = (timestamp - before.timestamp).toDouble() / span.toDouble()
            return LatLng(
                latitude = before.latitude!! + (after.latitude!! - before.latitude!!) * fraction,
                longitude = before.longitude!! + (after.longitude!! - before.longitude!!) * fraction,
            )
        }

        private fun RideTelemetry.hasLocation(): Boolean = latitude != null && longitude != null

        private fun RideTelemetry.withLocation(
            latitude: Double?,
            longitude: Double?,
        ): RideTelemetry = copy(latitude = latitude, longitude = longitude)

        private fun RideTelemetry.withLocation(latLng: LatLng): RideTelemetry =
            copy(latitude = latLng.latitude, longitude = latLng.longitude)

        /** 도메인 내부 좌표 쌍(외부 지도 SDK 의존 없이 순수 변환 유지). */
        private data class LatLng(
            val latitude: Double,
            val longitude: Double,
        )
    }
