package kr.yooreka.speedo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearAll()
}
