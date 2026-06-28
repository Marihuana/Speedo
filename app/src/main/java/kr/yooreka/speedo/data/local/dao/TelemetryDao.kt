package kr.yooreka.speedo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kr.yooreka.speedo.data.local.entity.TelemetryEntity

@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TelemetryEntity>)

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<TelemetryEntity>>

    @Query("SELECT * FROM telemetry_logs WHERE rideId = :rideId ORDER BY timestamp ASC")
    suspend fun getTelemetryByRideId(rideId: Long): List<TelemetryEntity>

    @Query("DELETE FROM telemetry_logs WHERE rideId = :rideId")
    suspend fun deleteByRideId(rideId: Long)

    /** 자동 종료 Trimming(F-18 §4.1): 정차 시점 이후(timestamp 초과) 텔레메트리 행을 삭제한다. */
    @Query("DELETE FROM telemetry_logs WHERE rideId = :rideId AND timestamp > :timestamp")
    suspend fun deleteByRideIdAfter(
        rideId: Long,
        timestamp: Long,
    )

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearAll()
}
