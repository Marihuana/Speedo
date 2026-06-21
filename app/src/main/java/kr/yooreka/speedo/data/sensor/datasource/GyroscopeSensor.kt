package kr.yooreka.speedo.data.sensor.datasource

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.yooreka.speedo.domain.model.GyroscopeData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TYPE_GYROSCOPE(각속도 rad/s) 데이터 소스. 상보 필터 전략(COMPLEMENTARY)의 고주파 입력이며,
 * 적분 dt 정확도를 위해 SensorEvent.timestamp(ns)를 함께 전달한다(F-03).
 */
@Singleton
class GyroscopeSensor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SensorEventListener, SensorDataSource<GyroscopeData> {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        private val _dataFlow = MutableStateFlow(GyroscopeData())
        override val dataFlow: StateFlow<GyroscopeData> = _dataFlow.asStateFlow()

        override fun start() {
            sensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        override fun stop() {
            sensorManager.unregisterListener(this)
            _dataFlow.value = GyroscopeData()
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return
            _dataFlow.value =
                GyroscopeData(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestampNanos = event.timestamp,
                )
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) = Unit
    }
