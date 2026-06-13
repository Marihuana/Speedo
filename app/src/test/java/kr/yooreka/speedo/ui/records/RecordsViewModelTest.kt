package kr.yooreka.speedo.ui.records

import app.cash.turbine.test
import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.usecase.DeleteRideUseCase
import kr.yooreka.speedo.domain.usecase.GetRideHistoryUseCase
import kr.yooreka.speedo.domain.usecase.UpdateRideTitleUseCase
import kr.yooreka.speedo.fake.FakeBillingRepository
import kr.yooreka.speedo.fake.FakeRideRepository
import kr.yooreka.speedo.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRideRepository()
    private val billing = FakeBillingRepository()

    private fun viewModel() = RecordsViewModel(
        getRideHistoryUseCase = GetRideHistoryUseCase(repository),
        updateRideTitleUseCase = UpdateRideTitleUseCase(repository),
        deleteRideUseCase = DeleteRideUseCase(repository),
        billingRepository = billing,
    )

    @Test
    fun `uiState maps rides to formatted records and sums total distance`() = runTest {
        repository.setRides(
            listOf(
                Ride(id = 1, title = "Ride A", startTime = 1_700_000_000_000L, totalDistance = 10.0f, maxLean = 30f, maxSpeed = 80f, duration = 3_661_000L),
                Ride(id = 2, title = "Ride B", startTime = 1_700_100_000_000L, totalDistance = 5.5f, maxLean = 12f, maxSpeed = 60f, duration = 60_000L),
            ),
        )

        viewModel().uiState.test {
            var state = awaitItem()
            while (state.records.isEmpty()) state = awaitItem()

            assertEquals(2, state.records.size)
            val a = state.records.first { it.id == 1L }
            assertEquals("Ride A", a.title)
            assertEquals(String.format("%.1f km", 10.0f), a.distance)
            assertEquals(String.format("%.0f°", 30f), a.maxLean)
            assertEquals(String.format("%.0f", 80f), a.topSpeed)
            assertEquals("01:01:01", a.duration)

            // 총 거리 = 10.0 + 5.5 = 15.5
            assertEquals(String.format("%.1f", 15.5), state.totalDistance)
            assertFalse(state.isAdRemoved)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `uiState reflects ad-removed state from billing`() = runTest {
        billing.setAdRemoved(true)
        repository.setRides(listOf(Ride(id = 1, title = "R", startTime = 0L, totalDistance = 1f)))

        viewModel().uiState.test {
            var state = awaitItem()
            while (state.records.isEmpty()) state = awaitItem()
            assertTrue(state.isAdRemoved)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `empty history yields empty records and zero total distance`() = runTest {
        viewModel().uiState.test {
            val state = awaitItem()
            assertTrue(state.records.isEmpty())
            assertEquals(String.format("%.1f", 0.0), state.totalDistance)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `renameRide trims and forwards a valid title`() = runTest {
        viewModel().renameRide(7, "  New Name  ")
        assertEquals(listOf(7L to "New Name"), repository.updateTitleCalls)
    }

    @Test
    fun `renameRide ignores blank titles`() = runTest {
        val vm = viewModel()
        vm.renameRide(7, "")
        vm.renameRide(7, "   ")
        assertTrue(repository.updateTitleCalls.isEmpty())
    }

    @Test
    fun `deleteRide forwards the id to the use case`() = runTest {
        viewModel().deleteRide(9)
        assertEquals(listOf(9L), repository.deleteCalls)
    }
}
