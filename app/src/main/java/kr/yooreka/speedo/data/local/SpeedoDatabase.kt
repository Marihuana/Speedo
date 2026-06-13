package kr.yooreka.speedo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.local.entity.TelemetryEntity

import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.entity.RideEntity

@Database(entities = [TelemetryEntity::class, RideEntity::class], version = 3, exportSchema = false)
abstract class SpeedoDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
    abstract fun rideDao(): RideDao
}
