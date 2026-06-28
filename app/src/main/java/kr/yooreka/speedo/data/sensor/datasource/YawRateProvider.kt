package kr.yooreka.speedo.data.sensor.datasource

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.domain.repository.YawRateMeasurement
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * 차량 yaw(수직축 회전) 요율을 산출해 노출하는 소스(F-03b).
 *
 * 기기 좌표계 자이로 각속도 벡터를 가속도(중력) 방향으로 투영([YawRateMath.aboutUpAxis])하여,
 * 거치 방향과 무관하게 수직축 기준 요율(rad/s)을 구한다. 자이로/가속도 센서는 참조 카운트로
 * 공유되므로 lean 전략과 동시에 가동되어도 안전하다.
 *
 * 데이터가 없으면(센서 미가동/부재) [Float.NaN] 을 방출한다. 소비처(LeanPhysicsGuard)는 이를
 * "검증 불가"로 보고 원시 뱅킹각을 보존한다(fail-open).
 */
@Singleton
class YawRateProvider
    @Inject
    constructor(
        private val gyroSensor: GyroscopeSensor,
        private val accelSensor: AccelerometerSensor,
    ) : YawRateMeasurement {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var job: Job? = null

        private val _yawRateStream = MutableStateFlow(Float.NaN)

        /** 수직축 기준 yaw 요율(rad/s). 데이터 없음은 NaN. */
        override val yawRateStream: StateFlow<Float> = _yawRateStream.asStateFlow()

        fun start() {
            if (job?.isActive == true) return
            gyroSensor.start()
            accelSensor.start()
            job =
                scope.launch {
                    gyroSensor.dataFlow.collect { gyro ->
                        if (gyro.timestampNanos == 0L) {
                            _yawRateStream.value = Float.NaN
                            return@collect
                        }
                        val accel = accelSensor.dataFlow.value
                        _yawRateStream.value =
                            YawRateMath.aboutUpAxis(
                                gyro.x,
                                gyro.y,
                                gyro.z,
                                accel.x,
                                accel.y,
                                accel.z,
                            )
                    }
                }
        }

        fun stop() {
            job?.cancel()
            job = null
            gyroSensor.stop()
            accelSensor.stop()
            _yawRateStream.value = Float.NaN
        }
    }

/**
 * yaw 요율 산출 순수 수학(F-03b). 자이로 각속도 벡터 ω 를 정규화된 중력(위) 방향 ĝ 에 투영한다:
 * `ω · ĝ`. 가속도 크기가 0에 가까우면(자유낙하/데이터 없음) 방향을 알 수 없어 NaN.
 */
internal object YawRateMath {
    private const val MIN_ACCEL_MAGNITUDE = 1e-3f

    fun aboutUpAxis(
        gx: Float,
        gy: Float,
        gz: Float,
        ax: Float,
        ay: Float,
        az: Float,
    ): Float {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        if (magnitude < MIN_ACCEL_MAGNITUDE) return Float.NaN
        return (gx * ax + gy * ay + gz * az) / magnitude
    }
}
