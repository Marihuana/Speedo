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
import kr.yooreka.speedo.domain.model.AccelerometerData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccelerometerSensor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SensorEventListener, SensorDataSource<AccelerometerData> {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        private val _dataFlow = MutableStateFlow(AccelerometerData())
        override val dataFlow: StateFlow<AccelerometerData> = _dataFlow.asStateFlow()

        override fun start() {
            sensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        override fun stop() {
            sensorManager.unregisterListener(this)
            _dataFlow.value = AccelerometerData()
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            _dataFlow.value =
                AccelerometerData(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestamp = System.currentTimeMillis(),
                )
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) = Unit
    }
