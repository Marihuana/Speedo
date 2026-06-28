package kr.yooreka.speedo.data.sensor.lean

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.sensor.datasource.AccelerometerSensor
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.repository.LeanProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TYPE_ACCELEROMETER + atan2 전략(F-03). 센서융합이 없는 원시 가속도라 선형가속도/진동 노이즈가
 * 크지만, 융합 보정의 영향을 받지 않는 원시 비교군이다.
 */
@Singleton
class AccelTiltLeanProvider
    @Inject
    constructor(
        private val accelSensor: AccelerometerSensor,
    ) : LeanProvider {
        override val mode = LeanMode.ACCEL_TILT

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var job: Job? = null

        private val _leanStream = MutableStateFlow(LeanMath.NO_DATA)
        override val leanStream: StateFlow<Float> = _leanStream.asStateFlow()

        override fun start() {
            if (job?.isActive == true) return
            accelSensor.start()
            job =
                scope.launch {
                    accelSensor.dataFlow.collect { a ->
                        _leanStream.value =
                            if (a.timestamp != 0L) LeanMath.rollFromUpVector(a.x, a.y, a.z) else LeanMath.NO_DATA
                    }
                }
        }

        override fun stop() {
            job?.cancel()
            job = null
            accelSensor.stop()
            _leanStream.value = LeanMath.NO_DATA
        }
    }
