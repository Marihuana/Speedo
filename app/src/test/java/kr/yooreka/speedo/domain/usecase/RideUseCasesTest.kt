package kr.yooreka.speedo.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.fake.FakeRideRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * UseCase는 Repository 위임 + 타입 계약(Flow / Result)을 보장하는 얇은 계층이므로,
 * Fake Repository로 위임 동작과 결과 전달을 검증한다.
 */
class RideUseCasesTest {
    private val repository = FakeRideRepository()

    private fun ride(id: Long) = Ride(id = id, title = "R$id", startTime = id)

    @Test
    fun `GetRideHistoryUseCase exposes the repository ride stream`() =
        runTest {
            val useCase = GetRideHistoryUseCase(repository)
            repository.setRides(listOf(ride(1), ride(2)))

            useCase().test {
                assertEquals(listOf(1L, 2L), awaitItem().map { it.id })
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `GetRideDetailUseCase delegates success`() =
        runTest {
            repository.setRides(listOf(ride(3)))
            val result = GetRideDetailUseCase(repository)(3)
            assertTrue(result.isSuccess)
            assertEquals(3L, result.getOrNull()?.id)
        }

    @Test
    fun `GetRideDetailUseCase propagates failure`() =
        runTest {
            repository.getRideOverride = { Result.failure(IOException("boom")) }
            val result = GetRideDetailUseCase(repository)(1)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IOException)
        }

    @Test
    fun `GetRideTelemetryUseCase delegates to repository`() =
        runTest {
            val points =
                listOf(
                    RideTelemetry(
                        rideId = 1,
                        timestamp = 1,
                        speed = 5f,
                        roll = 0f,
                        brakeEvent = BrakeEvent.NONE,
                        brakeForce = 0f,
                        latitude = null,
                        longitude = null,
                    ),
                )
            repository.telemetryByRide = mapOf(1L to points)

            val result = GetRideTelemetryUseCase(repository)(1)

            assertEquals(points, result.getOrThrow())
        }

    @Test
    fun `UpdateRideTitleUseCase forwards arguments and returns result`() =
        runTest {
            val useCase = UpdateRideTitleUseCase(repository)

            val result = useCase(7, "New Title")

            assertTrue(result.isSuccess)
            assertEquals(listOf(7L to "New Title"), repository.updateTitleCalls)
        }

    @Test
    fun `UpdateRideTitleUseCase propagates repository failure`() =
        runTest {
            repository.updateTitleResult = Result.failure(IOException("boom"))
            assertTrue(UpdateRideTitleUseCase(repository)(1, "x").isFailure)
        }

    @Test
    fun `DeleteRideUseCase forwards id and returns result`() =
        runTest {
            val result = DeleteRideUseCase(repository)(9)

            assertTrue(result.isSuccess)
            assertEquals(listOf(9L), repository.deleteCalls)
        }

    @Test
    fun `DeleteRideUseCase propagates repository failure`() =
        runTest {
            repository.deleteResult = Result.failure(IOException("boom"))
            assertTrue(DeleteRideUseCase(repository)(1).isFailure)
        }
}
