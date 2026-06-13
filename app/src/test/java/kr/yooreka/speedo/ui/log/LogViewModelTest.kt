package kr.yooreka.speedo.ui.log

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.domain.usecase.GetRideDetailUseCase
import kr.yooreka.speedo.domain.usecase.GetRideTelemetryUseCase
import kr.yooreka.speedo.fake.FakeRideRepository
import kr.yooreka.speedo.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRideRepository()

    private fun viewModel(rideId: Long?) =
        LogViewModel(
            savedStateHandle =
                SavedStateHandle(
                    if (rideId == null) emptyMap() else mapOf("rideId" to rideId),
                ),
            getRideDetailUseCase = GetRideDetailUseCase(repository),
            getRideTelemetryUseCase = GetRideTelemetryUseCase(repository),
        )

    private fun point(
        ts: Long,
        speed: Float,
    ) = RideTelemetry(
        rideId = 1,
        timestamp = ts,
        speed = speed,
        roll = 0f,
        brakeEvent = BrakeEvent.NONE,
        brakeForce = 0f,
        latitude = 1.0,
        longitude = 2.0,
    )

    @Test
    fun `valid ride id populates state with formatted fields and route`() {
        val ride =
            Ride(
                id = 1,
                title = "Sunset Run",
                startTime = 1_700_000_000_000L,
                totalDistance = 12.34f,
                maxLean = 37.6f,
                duration = 3_661_000L,
            )
        repository.setRides(listOf(ride))
        repository.telemetryByRide = mapOf(1L to listOf(point(1, 20f), point(2, 88f), point(3, 55f)))

        val state = viewModel(1).uiState.value

        assertFalse(state.isLoading)
        assertEquals("Sunset Run", state.title)
        val expectedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(ride.startTime))
        assertEquals(expectedDate, state.date)
        assertEquals(String.format("%.1f", 12.34f), state.distance)
        assertEquals(String.format("%.0f", 37.6f), state.maxLean)
        assertEquals("88", state.maxSpeed) // 텔레메트리 최대 속도
        assertEquals("01:01:01", state.duration) // 3,661,000ms = 1h 1m 1s
        assertEquals(3, state.routePoints.size)
    }

    @Test
    fun `max speed is zero when telemetry is empty`() {
        repository.setRides(listOf(Ride(id = 1, title = "Empty", startTime = 0L)))
        repository.telemetryByRide = emptyMap()

        val state = viewModel(1).uiState.value

        assertEquals("0", state.maxSpeed)
        assertTrue(state.routePoints.isEmpty())
    }

    @Test
    fun `invalid ride id surfaces invalid id error and stops loading`() {
        val state = viewModel(-1L).uiState.value

        assertFalse(state.isLoading)
        assertEquals("Error: Invalid Ride ID", state.title)
        assertTrue(state.routePoints.isEmpty())
    }

    @Test
    fun `missing ride id argument is treated as invalid`() {
        val state = viewModel(null).uiState.value
        assertEquals("Error: Invalid Ride ID", state.title)
    }

    @Test
    fun `ride not found surfaces record not found`() {
        // 저장소가 비어 있어 getRide가 실패(Result.failure)를 반환
        val state = viewModel(42L).uiState.value

        assertFalse(state.isLoading)
        assertEquals("Record not found", state.title)
    }

    @Test
    fun `repository error surfaces record not found`() {
        repository.getRideOverride = { Result.failure(IOException("db down")) }
        val state = viewModel(1L).uiState.value
        assertEquals("Record not found", state.title)
    }

    @Test
    fun `selectPoint updates and clears the selected point`() =
        runTest {
            repository.setRides(listOf(Ride(id = 1, title = "R", startTime = 0L)))
            val vm = viewModel(1)
            val p = point(1, 30f)

            vm.selectPoint(p)
            assertEquals(p, vm.uiState.value.selectedPoint)

            vm.selectPoint(null)
            assertNull(vm.uiState.value.selectedPoint)
        }
}
