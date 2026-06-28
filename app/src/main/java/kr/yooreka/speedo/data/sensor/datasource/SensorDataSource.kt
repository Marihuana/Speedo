package kr.yooreka.speedo.data.sensor.datasource

import kotlinx.coroutines.flow.StateFlow

/**
 * 모든 센서 데이터 소스의 공통 인터페이스
 */
interface SensorDataSource<T> {
    val dataFlow: StateFlow<T>

    fun start()

    fun stop()
}
