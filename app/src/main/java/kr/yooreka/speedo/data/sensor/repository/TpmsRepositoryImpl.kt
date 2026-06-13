package kr.yooreka.speedo.data.sensor.repository

import kotlinx.coroutines.flow.StateFlow
import kr.yooreka.speedo.data.sensor.datasource.TpmsDataSource
import kr.yooreka.speedo.domain.model.TpmsData
import kr.yooreka.speedo.domain.repository.SensorRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TpmsRepositoryImpl
    @Inject
    constructor(
        private val dataSource: TpmsDataSource,
    ) : SensorRepository<TpmsData> {
        override val dataStream: StateFlow<TpmsData> = dataSource.dataFlow

        override fun start() = dataSource.start()

        override fun stop() = dataSource.stop()
    }
