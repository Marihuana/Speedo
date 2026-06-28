package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.RideTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 선형 경로 보간(F-13c) 단위 테스트. 좌표 채움만 검증하고 speed/roll/brake 등은 보존되어야 한다.
 */
class LinearPathInterpolatorTest {
    private val interpolator = LinearPathInterpolator()

    private val epsilon = 1e-9

    private fun point(
        timestamp: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        speed: Float = 0f,
        roll: Float = 0f,
        brakeEvent: BrakeEvent = BrakeEvent.NONE,
        brakeForce: Float = 0f,
    ) = RideTelemetry(
        rideId = 1,
        timestamp = timestamp,
        speed = speed,
        roll = roll,
        brakeEvent = brakeEvent,
        brakeForce = brakeForce,
        latitude = latitude,
        longitude = longitude,
    )

    @Test
    fun `empty input returns empty`() {
        assertEquals(emptyList<RideTelemetry>(), interpolator.interpolate(emptyList()))
    }

    @Test
    fun `zero anchors returns input unchanged with null coordinates`() {
        val input = listOf(point(0), point(1), point(2))

        val result = interpolator.interpolate(input)

        assertEquals(input, result)
        result.forEach {
            assertNull(it.latitude)
            assertNull(it.longitude)
        }
    }

    @Test
    fun `single anchor fills all rows with that anchor coordinate`() {
        val input =
            listOf(
                point(0),
                point(1, latitude = 10.0, longitude = 20.0),
                point(2),
            )

        val result = interpolator.interpolate(input)

        result.forEach {
            assertEquals(10.0, it.latitude!!, epsilon)
            assertEquals(20.0, it.longitude!!, epsilon)
        }
    }

    @Test
    fun `two anchors interpolate evenly spaced midpoint`() {
        val input =
            listOf(
                point(0, latitude = 0.0, longitude = 0.0),
                // t=5 는 정중앙(f=0.5) → lat 5.0, lng 10.0
                point(5),
                point(10, latitude = 10.0, longitude = 20.0),
            )

        val result = interpolator.interpolate(input)

        assertEquals(5.0, result[1].latitude!!, epsilon)
        assertEquals(10.0, result[1].longitude!!, epsilon)
    }

    @Test
    fun `non-uniform timestamps interpolate proportionally`() {
        val input =
            listOf(
                point(100, latitude = 0.0, longitude = 0.0),
                // f = (175-100)/(300-100) = 0.375
                point(175),
                point(300, latitude = 8.0, longitude = 40.0),
            )

        val result = interpolator.interpolate(input)

        assertEquals(0.375 * 8.0, result[1].latitude!!, epsilon)
        assertEquals(0.375 * 40.0, result[1].longitude!!, epsilon)
    }

    @Test
    fun `rows before first anchor clamp to first anchor`() {
        val input =
            listOf(
                point(0),
                point(1),
                point(2, latitude = 3.0, longitude = 4.0),
                point(3, latitude = 5.0, longitude = 6.0),
            )

        val result = interpolator.interpolate(input)

        // index 0,1 은 첫 앵커(index 2) 좌표로 클램프(외삽 금지)
        assertEquals(3.0, result[0].latitude!!, epsilon)
        assertEquals(4.0, result[0].longitude!!, epsilon)
        assertEquals(3.0, result[1].latitude!!, epsilon)
        assertEquals(4.0, result[1].longitude!!, epsilon)
    }

    @Test
    fun `rows after last anchor clamp to last anchor`() {
        val input =
            listOf(
                point(0, latitude = 3.0, longitude = 4.0),
                point(1, latitude = 5.0, longitude = 6.0),
                point(2),
                point(3),
            )

        val result = interpolator.interpolate(input)

        assertEquals(5.0, result[2].latitude!!, epsilon)
        assertEquals(6.0, result[2].longitude!!, epsilon)
        assertEquals(5.0, result[3].latitude!!, epsilon)
        assertEquals(6.0, result[3].longitude!!, epsilon)
    }

    @Test
    fun `equal anchor timestamps avoid divide by zero and use earlier anchor`() {
        val input =
            listOf(
                point(10, latitude = 1.0, longitude = 2.0),
                // null 행, 분모(tB-tA)=0
                point(10),
                point(10, latitude = 9.0, longitude = 9.0),
            )

        val result = interpolator.interpolate(input)

        assertEquals(1.0, result[1].latitude!!, epsilon)
        assertEquals(2.0, result[1].longitude!!, epsilon)
    }

    @Test
    fun `interpolation preserves speed roll and brake fields`() {
        val input =
            listOf(
                point(0, latitude = 0.0, longitude = 0.0, speed = 40f, roll = -12f),
                point(
                    5,
                    speed = 88f,
                    roll = 33f,
                    brakeEvent = BrakeEvent.HARD,
                    brakeForce = 7.5f,
                ),
                point(10, latitude = 10.0, longitude = 10.0, speed = 60f, roll = 5f),
            )

        val result = interpolator.interpolate(input)

        val middle = result[1]
        assertEquals(88f, middle.speed)
        assertEquals(33f, middle.roll)
        assertEquals(BrakeEvent.HARD, middle.brakeEvent)
        assertEquals(7.5f, middle.brakeForce)
        // 좌표는 채워졌는지만 확인
        assertEquals(5.0, middle.latitude!!, epsilon)
        assertEquals(5.0, middle.longitude!!, epsilon)
    }

    @Test
    fun `multiple null rows across multiple anchor segments interpolate per segment`() {
        val input =
            listOf(
                point(0, latitude = 0.0, longitude = 0.0),
                // segment [0,4]: f=0.5
                point(2),
                point(4, latitude = 4.0, longitude = 0.0),
                // segment [4,10]: f=0.5
                point(7),
                point(10, latitude = 4.0, longitude = 6.0),
            )

        val result = interpolator.interpolate(input)

        assertEquals(2.0, result[1].latitude!!, epsilon)
        assertEquals(0.0, result[1].longitude!!, epsilon)
        assertEquals(4.0, result[3].latitude!!, epsilon)
        assertEquals(3.0, result[3].longitude!!, epsilon)
    }
}
