package kr.yooreka.speedo.data.mapper

import kr.yooreka.speedo.data.local.entity.RideEntity
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kr.yooreka.speedo.domain.model.BrakeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideMappersTest {

    @Test
    fun `RideEntity toDomain maps every field`() {
        val entity = RideEntity(
            id = 42L,
            title = "Morning Ride",
            startTime = 1_000L,
            endTime = 5_000L,
            totalDistance = 12.3f,
            maxLean = 38f,
            maxSpeed = 99.5f,
            duration = 4_000L,
        )

        val ride = entity.toDomain()

        assertEquals(42L, ride.id)
        assertEquals("Morning Ride", ride.title)
        assertEquals(1_000L, ride.startTime)
        assertEquals(5_000L, ride.endTime)
        assertEquals(12.3f, ride.totalDistance, 0.0001f)
        assertEquals(38f, ride.maxLean, 0.0001f)
        assertEquals(99.5f, ride.maxSpeed, 0.0001f)
        assertEquals(4_000L, ride.duration)
    }

    @Test
    fun `RideEntity toDomain preserves null endTime`() {
        val ride = RideEntity(title = "In progress", startTime = 1L, endTime = null).toDomain()
        assertNull(ride.endTime)
    }

    @Test
    fun `TelemetryEntity toDomain maps every field including coordinates and brake`() {
        val entity = TelemetryEntity(
            id = 7L,
            rideId = 42L,
            timestamp = 1_234L,
            speed = 55.5f,
            roll = -12.5f,
            brakeEvent = BrakeEvent.HARD,
            brakeForce = 0.8f,
            latitude = 37.5,
            longitude = 127.0,
        )

        val point = entity.toDomain()

        assertEquals(7L, point.id)
        assertEquals(42L, point.rideId)
        assertEquals(1_234L, point.timestamp)
        assertEquals(55.5f, point.speed, 0.0001f)
        assertEquals(-12.5f, point.roll, 0.0001f)
        assertEquals(BrakeEvent.HARD, point.brakeEvent)
        assertEquals(0.8f, point.brakeForce, 0.0001f)
        assertEquals(37.5, point.latitude!!, 0.0001)
        assertEquals(127.0, point.longitude!!, 0.0001)
    }

    @Test
    fun `TelemetryEntity toDomain preserves null coordinates`() {
        val point = TelemetryEntity(
            rideId = 1L,
            timestamp = 0L,
            speed = 0f,
            roll = 0f,
            brakeEvent = BrakeEvent.NONE,
            brakeForce = 0f,
            latitude = null,
            longitude = null,
        ).toDomain()

        assertNull(point.latitude)
        assertNull(point.longitude)
        assertEquals(BrakeEvent.NONE, point.brakeEvent)
    }
}
