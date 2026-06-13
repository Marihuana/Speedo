package kr.yooreka.speedo.data.sensor.repository

import kr.yooreka.speedo.data.sensor.datasource.LocationDataSource
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.repository.SensorRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val dataSource: LocationDataSource
) : SensorRepository<LocationData> {
    override val dataStream: StateFlow<LocationData> = dataSource.dataFlow
    override fun start() = dataSource.start()
    override fun stop() = dataSource.stop()
}
