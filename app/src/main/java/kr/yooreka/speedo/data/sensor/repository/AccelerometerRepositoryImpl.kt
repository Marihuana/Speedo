package kr.yooreka.speedo.data.sensor.repository

import kr.yooreka.speedo.data.sensor.datasource.AccelerometerSensor
import kr.yooreka.speedo.domain.model.AccelerometerData
import kr.yooreka.speedo.domain.repository.SensorRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccelerometerRepositoryImpl @Inject constructor(
    private val dataSource: AccelerometerSensor
) : SensorRepository<AccelerometerData> {
    override val dataStream: StateFlow<AccelerometerData> = dataSource.dataFlow
    override fun start() = dataSource.start()
    override fun stop() = dataSource.stop()
}
