package kr.yooreka.speedo.data.sensor.repository

import kr.yooreka.speedo.data.sensor.datasource.GravitySensor
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.repository.SensorRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GravityRepositoryImpl @Inject constructor(
    private val dataSource: GravitySensor
) : SensorRepository<GravityData> {
    override val dataStream: StateFlow<GravityData> = dataSource.dataFlow
    override fun start() = dataSource.start()
    override fun stop() = dataSource.stop()
}
