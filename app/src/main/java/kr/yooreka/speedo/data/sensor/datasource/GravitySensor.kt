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
import kr.yooreka.speedo.domain.model.GravityData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GravitySensor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SensorEventListener, SensorDataSource<GravityData> {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        private val _dataFlow = MutableStateFlow(GravityData())
        override val dataFlow: StateFlow<GravityData> = _dataFlow.asStateFlow()

        override fun start() {
            sensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        override fun stop() {
            sensorManager.unregisterListener(this)
            _dataFlow.value = GravityData()
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_GRAVITY) return
            _dataFlow.value =
                GravityData(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                )
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) = Unit
    }
