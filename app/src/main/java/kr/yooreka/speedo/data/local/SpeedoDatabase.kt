package kr.yooreka.speedo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.local.entity.RideEntity
import kr.yooreka.speedo.data.local.entity.TelemetryEntity

@Database(entities = [TelemetryEntity::class, RideEntity::class], version = 4, exportSchema = false)
abstract class SpeedoDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao

    abstract fun rideDao(): RideDao
}
