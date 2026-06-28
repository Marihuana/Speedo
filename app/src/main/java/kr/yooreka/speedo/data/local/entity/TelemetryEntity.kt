package kr.yooreka.speedo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.LeanConfidence

@Entity(tableName = "telemetry_logs")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: Long,
    val timestamp: Long,
    val speed: Float,
    val roll: Float,
    val brakeEvent: BrakeEvent,
    val brakeForce: Float,
    val latitude: Double?,
    val longitude: Double?,
    /** 뱅킹각 데이터 신뢰도(F-03b). 기본값은 VALID. */
    val leanConfidence: LeanConfidence = LeanConfidence.VALID,
)
