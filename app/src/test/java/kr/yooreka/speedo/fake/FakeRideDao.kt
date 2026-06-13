package kr.yooreka.speedo.fake

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.entity.RideEntity

/**
 * 인메모리 [RideDao] Fake. 결정적 테스트를 위해 실제 Room 동작(정렬/upsert)을 모사한다.
 *
 * @param ops 호출 순서를 검증해야 하는 테스트에서 여러 DAO가 공유하는 연산 로그.
 */
class FakeRideDao(
    private val ops: MutableList<String> = mutableListOf(),
) : RideDao {
    private val rides = MutableStateFlow<List<RideEntity>>(emptyList())
    private var nextId = 1L

    /** 읽기/쓰기 시 강제로 던질 예외(에러 경로 테스트용). */
    var throwOnRead: Throwable? = null
    var throwOnWrite: Throwable? = null

    fun seed(vararg entities: RideEntity) {
        rides.value = rides.value + entities
    }

    fun current(): List<RideEntity> = rides.value

    override suspend fun insertRide(ride: RideEntity): Long {
        throwOnWrite?.let { throw it }
        val id = if (ride.id != 0L) ride.id else nextId++
        rides.value = rides.value.filterNot { it.id == id } + ride.copy(id = id)
        return id
    }

    override suspend fun updateRide(ride: RideEntity) {
        throwOnWrite?.let { throw it }
        rides.value = rides.value.map { if (it.id == ride.id) ride else it }
    }

    override suspend fun updateTitle(
        rideId: Long,
        title: String,
    ) {
        throwOnWrite?.let { throw it }
        rides.value = rides.value.map { if (it.id == rideId) it.copy(title = title) else it }
    }

    // 실제 쿼리: ORDER BY startTime DESC
    override fun getAllRides(): Flow<List<RideEntity>> = rides.map { list -> list.sortedByDescending { it.startTime } }

    override suspend fun getRideById(rideId: Long): RideEntity? {
        throwOnRead?.let { throw it }
        return rides.value.firstOrNull { it.id == rideId }
    }

    override suspend fun deleteRide(rideId: Long) {
        throwOnWrite?.let { throw it }
        ops += "ride.deleteRide"
        rides.value = rides.value.filterNot { it.id == rideId }
    }
}
