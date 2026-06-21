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
import kr.yooreka.speedo.domain.model.RotationVectorData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TYPE_ROTATION_VECTOR(가속도+자이로+자력계 융합) 데이터 소스.
 * 자력계를 포함하므로 절대 방위(북) 기준이나, 바이크 자기 노이즈에 취약할 수 있다(F-03 비교군).
 */
@Singleton
class RotationVectorSensor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SensorEventListener, SensorDataSource<RotationVectorData> {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        private val _dataFlow = MutableStateFlow(RotationVectorData())
        override val dataFlow: StateFlow<RotationVectorData> = _dataFlow.asStateFlow()

        override fun start() {
            sensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        override fun stop() {
            sensorManager.unregisterListener(this)
            _dataFlow.value = RotationVectorData()
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            // values: [0..2]=쿼터니언 xyz, [3]=w(기기/버전에 따라 없을 수 있음).
            val w = if (event.values.size >= 4) event.values[3] else 0f
            _dataFlow.value =
                RotationVectorData(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    w = w,
                    hasData = true,
                )
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) = Unit
    }
