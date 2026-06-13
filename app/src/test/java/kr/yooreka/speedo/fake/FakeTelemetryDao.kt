package kr.yooreka.speedo.fake

import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 인메모리 [TelemetryDao] Fake.
 *
 * @param ops 호출 순서를 검증해야 하는 테스트에서 여러 DAO가 공유하는 연산 로그.
 */
class FakeTelemetryDao(
    private val ops: MutableList<String> = mutableListOf(),
) : TelemetryDao {

    private val logs = MutableStateFlow<List<TelemetryEntity>>(emptyList())

    var throwOnRead: Throwable? = null
    var throwOnWrite: Throwable? = null

    fun seed(vararg entities: TelemetryEntity) {
        logs.value = logs.value + entities
    }

    fun current(): List<TelemetryEntity> = logs.value

    override suspend fun insertAll(entities: List<TelemetryEntity>) {
        throwOnWrite?.let { throw it }
        logs.value = logs.value + entities
    }

    override fun getAllLogs(): Flow<List<TelemetryEntity>> = logs

    // 실제 쿼리: WHERE rideId = :rideId ORDER BY timestamp ASC
    override suspend fun getTelemetryByRideId(rideId: Long): List<TelemetryEntity> {
        throwOnRead?.let { throw it }
        return logs.value.filter { it.rideId == rideId }.sortedBy { it.timestamp }
    }

    override suspend fun deleteByRideId(rideId: Long) {
        throwOnWrite?.let { throw it }
        ops += "telemetry.deleteByRideId"
        logs.value = logs.value.filterNot { it.rideId == rideId }
    }

    override suspend fun clearAll() {
        throwOnWrite?.let { throw it }
        logs.value = emptyList()
    }
}
