package kr.yooreka.speedo.data.repository

import app.cash.turbine.test
import kr.yooreka.speedo.data.local.entity.RideEntity
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.fake.FakeRideDao
import kr.yooreka.speedo.fake.FakeTelemetryDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RideRepositoryImplTest {

    private val rideDao = FakeRideDao()
    private val telemetryDao = FakeTelemetryDao()
    private val repository = RideRepositoryImpl(rideDao, telemetryDao)

    private fun ride(id: Long, title: String = "R$id", startTime: Long = id * 100) =
        RideEntity(id = id, title = title, startTime = startTime, totalDistance = 1f)

    private fun point(rideId: Long, ts: Long, speed: Float = 10f) = TelemetryEntity(
        rideId = rideId, timestamp = ts, speed = speed, roll = 0f,
        brakeEvent = BrakeEvent.NONE, brakeForce = 0f, latitude = 1.0, longitude = 2.0,
    )

    // ---------- observeRides ----------

    @Test
    fun `observeRides maps entities to domain sorted by startTime desc`() = runTest {
        rideDao.seed(ride(1, startTime = 100), ride(2, startTime = 300), ride(3, startTime = 200))

        repository.observeRides().test {
            val rides = awaitItem()
            assertEquals(listOf(2L, 3L, 1L), rides.map { it.id })
            assertEquals("R2", rides.first().title)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `observeRides emits update when a new ride is inserted`() = runTest {
        repository.observeRides().test {
            assertTrue(awaitItem().isEmpty())

            rideDao.seed(ride(1))
            assertEquals(listOf(1L), awaitItem().map { it.id })

            cancelAndConsumeRemainingEvents()
        }
    }

    // ---------- getRide ----------

    @Test
    fun `getRide returns success with mapped domain model`() = runTest {
        rideDao.seed(ride(5, title = "Coast Run"))

        val result = repository.getRide(5)

        assertTrue(result.isSuccess)
        assertEquals("Coast Run", result.getOrNull()?.title)
        assertEquals(5L, result.getOrNull()?.id)
    }

    @Test
    fun `getRide returns failure when ride not found`() = runTest {
        val result = repository.getRide(999)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun `getRide returns failure when dao throws`() = runTest {
        rideDao.throwOnRead = IOException("db error")

        val result = repository.getRide(1)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    // ---------- getRideTelemetry ----------

    @Test
    fun `getRideTelemetry returns mapped points sorted by timestamp asc`() = runTest {
        telemetryDao.seed(point(1, ts = 30), point(1, ts = 10), point(1, ts = 20), point(2, ts = 5))

        val result = repository.getRideTelemetry(1)

        assertTrue(result.isSuccess)
        val points = result.getOrThrow()
        assertEquals(3, points.size)
        assertEquals(listOf(10L, 20L, 30L), points.map { it.timestamp })
        assertTrue(points.all { it.rideId == 1L })
    }

    @Test
    fun `getRideTelemetry returns empty list when no points`() = runTest {
        val result = repository.getRideTelemetry(1)
        assertEquals(emptyList<Long>(), result.getOrThrow().map { it.timestamp })
    }

    @Test
    fun `getRideTelemetry returns failure when dao throws`() = runTest {
        telemetryDao.throwOnRead = IOException("db error")
        assertTrue(repository.getRideTelemetry(1).isFailure)
    }

    // ---------- updateRideTitle ----------

    @Test
    fun `updateRideTitle updates the row and returns success`() = runTest {
        rideDao.seed(ride(1, title = "Old"))

        val result = repository.updateRideTitle(1, "New")

        assertTrue(result.isSuccess)
        assertEquals("New", rideDao.current().first { it.id == 1L }.title)
    }

    @Test
    fun `updateRideTitle returns failure when dao throws`() = runTest {
        rideDao.throwOnWrite = IOException("db error")
        assertTrue(repository.updateRideTitle(1, "x").isFailure)
    }

    // ---------- deleteRide ----------

    @Test
    fun `deleteRide removes both ride and its telemetry`() = runTest {
        rideDao.seed(ride(1), ride(2))
        telemetryDao.seed(point(1, ts = 1), point(1, ts = 2), point(2, ts = 1))

        val result = repository.deleteRide(1)

        assertTrue(result.isSuccess)
        assertFalse(rideDao.current().any { it.id == 1L })
        assertTrue(rideDao.current().any { it.id == 2L })
        assertEquals(listOf(2L), telemetryDao.current().map { it.rideId }) // 오직 ride 2 의 포인트만 남음
    }

    @Test
    fun `deleteRide deletes telemetry before the ride row`() = runTest {
        val ops = mutableListOf<String>()
        val orderedRideDao = FakeRideDao(ops)
        val orderedTelemetryDao = FakeTelemetryDao(ops)
        val repo = RideRepositoryImpl(orderedRideDao, orderedTelemetryDao)
        orderedRideDao.seed(ride(1))

        repo.deleteRide(1)

        assertEquals(listOf("telemetry.deleteByRideId", "ride.deleteRide"), ops)
    }

    @Test
    fun `deleteRide returns failure when dao throws`() = runTest {
        telemetryDao.throwOnWrite = IOException("db error")
        assertTrue(repository.deleteRide(1).isFailure)
    }
}
