package kr.yooreka.speedo.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    /**
     * v3 → v4: 텔레메트리에 뱅킹각 신뢰도 컬럼 추가(F-03b). 기존 행은 RELIABLE 로 채운다.
     * Room 은 enum 을 이름(TEXT)으로 저장하므로 기본값도 enum 상수명과 일치시킨다.
     */
    private val migration3To4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE telemetry_logs ADD COLUMN leanConfidence TEXT NOT NULL DEFAULT 'RELIABLE'",
                )
            }
        }

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
            .addMigrations(migration3To4)
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
