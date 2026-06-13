package kr.yooreka.speedo.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.yooreka.speedo.data.local.SpeedoDatabase
import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SpeedoDatabase {
        return Room.databaseBuilder(
            context,
            SpeedoDatabase::class.java,
            "speedo_database",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTelemetryDao(database: SpeedoDatabase): TelemetryDao {
        return database.telemetryDao()
    }

    @Provides
    fun provideRideDao(database: SpeedoDatabase): RideDao {
        return database.rideDao()
    }
}
