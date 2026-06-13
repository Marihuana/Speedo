package kr.yooreka.speedo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.yooreka.speedo.data.sensor.repository.AccelerometerRepositoryImpl
import kr.yooreka.speedo.data.sensor.repository.GravityRepositoryImpl
import kr.yooreka.speedo.data.sensor.repository.LeanCalibrationRepositoryImpl
import kr.yooreka.speedo.data.sensor.repository.LocationRepositoryImpl
import kr.yooreka.speedo.data.sensor.repository.TelemetryRepositoryImpl
import kr.yooreka.speedo.data.sensor.repository.TpmsRepositoryImpl
import kr.yooreka.speedo.domain.model.AccelerometerData
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.model.TpmsData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccelerometerRepository(
        impl: AccelerometerRepositoryImpl
    ): SensorRepository<AccelerometerData>

    @Binds
    @Singleton
    abstract fun bindGravityRepository(
        impl: GravityRepositoryImpl
    ): SensorRepository<GravityData>

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl
    ): SensorRepository<LocationData>

    @Binds
    @Singleton
    abstract fun bindTpmsRepository(
        impl: TpmsRepositoryImpl
    ): SensorRepository<TpmsData>

    @Binds
    @Singleton
    abstract fun bindTelemetryRepository(
        impl: TelemetryRepositoryImpl
    ): TelemetryRepository

    @Binds
    @Singleton
    abstract fun bindLeanCalibrationRepository(
        impl: LeanCalibrationRepositoryImpl
    ): LeanCalibrationRepository
}
