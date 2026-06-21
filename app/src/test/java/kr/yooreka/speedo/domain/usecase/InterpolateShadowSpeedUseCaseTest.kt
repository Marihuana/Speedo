package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.RideTelemetry
import org.junit.Assert.assertEquals
import org.junit.Test

class InterpolateShadowSpeedUseCaseTest {
    private val useCase = InterpolateShadowSpeedUseCase()

    private fun point(
        ts: Long,
        speed: Float,
        lat: Double? = null,
        lng: Double? = null,
    ) = RideTelemetry(
        rideId = 1,
        timestamp = ts,
        speed = speed,
        roll = 0f,
        brakeEvent = BrakeEvent.NONE,
        brakeForce = 0f,
        latitude = lat,
        longitude = lng,
    )

    @Test
    fun `fills interior speed with back-calculated average over a shadow gap`() {
        // 진입 (0,0) → 진출 (0, 0.0009): 적도 기준 약 100m. 4초 경과 → 약 90km/h.
        val points =
            listOf(
                point(0L, 30f, lat = 0.0, lng = 0.0),
                point(1000L, 0f),
                point(2000L, 0f),
                point(3000L, 0f),
                point(4000L, 40f, lat = 0.0, lng = 0.0009),
            )

        val result = useCase(points)

        // 앵커 속도는 보존
        assertEquals(30f, result.first().speed, 0.001f)
        assertEquals(40f, result.last().speed, 0.001f)
        // 내부(음영) 행은 역산 평균속도(≈90km/h)로 채워짐
        assertEquals(90.0f, result[1].speed, 2.0f)
        assertEquals(result[1].speed, result[2].speed, 0.001f)
        assertEquals(result[1].speed, result[3].speed, 0.001f)
    }

    @Test
    fun `leaves speed untouched when anchor gap is below threshold`() {
        // 정상 GPS 수신(1초 간격)은 실측 속도를 보존한다.
        val points =
            listOf(
                point(0L, 30f, lat = 0.0, lng = 0.0),
                point(200L, 5f),
                point(1000L, 33f, lat = 0.0, lng = 0.0009),
            )

        val result = useCase(points)

        assertEquals(5f, result[1].speed, 0.001f)
    }

    @Test
    fun `returns input unchanged when fewer than two anchors`() {
        val points =
            listOf(
                point(0L, 10f, lat = 1.0, lng = 2.0),
                point(5000L, 0f),
            )

        val result = useCase(points)

        assertEquals(points, result)
    }
}
