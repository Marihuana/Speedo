package kr.yooreka.speedo.fake

import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [RideRepository] Fake. UseCase / ViewModel 테스트에서 도메인 경계를 결정적으로 대체한다.
 */
class FakeRideRepository : RideRepository {

    private val ridesFlow = MutableStateFlow<List<Ride>>(emptyList())

    /** rideId → 텔레메트리. 미지정 시 빈 리스트. */
    var telemetryByRide: Map<Long, List<RideTelemetry>> = emptyMap()

    /** 지정 시 getRide 결과를 강제(에러 경로 등). null이면 ridesFlow에서 조회. */
    var getRideOverride: (suspend (Long) -> Result<Ride>)? = null
    var getTelemetryOverride: (suspend (Long) -> Result<List<RideTelemetry>>)? = null
    var updateTitleResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    // 호출 기록(상호작용 검증용)
    val updateTitleCalls = mutableListOf<Pair<Long, String>>()
    val deleteCalls = mutableListOf<Long>()

    fun setRides(rides: List<Ride>) {
        ridesFlow.value = rides
    }

    override fun observeRides(): Flow<List<Ride>> = ridesFlow

    override suspend fun getRide(rideId: Long): Result<Ride> {
        getRideOverride?.let { return it(rideId) }
        return ridesFlow.value.firstOrNull { it.id == rideId }
            ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Ride not found: id=$rideId"))
    }

    override suspend fun getRideTelemetry(rideId: Long): Result<List<RideTelemetry>> {
        getTelemetryOverride?.let { return it(rideId) }
        return Result.success(telemetryByRide[rideId] ?: emptyList())
    }

    override suspend fun updateRideTitle(rideId: Long, title: String): Result<Unit> {
        updateTitleCalls += rideId to title
        return updateTitleResult
    }

    override suspend fun deleteRide(rideId: Long): Result<Unit> {
        deleteCalls += rideId
        return deleteResult
    }
}
