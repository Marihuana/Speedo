package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SensorRepository<T> {
    val dataStream: StateFlow<T>

    fun start()

    fun stop()
}
