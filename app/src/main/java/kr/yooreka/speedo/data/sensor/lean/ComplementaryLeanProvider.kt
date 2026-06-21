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
import kr.yooreka.speedo.data.sensor.datasource.GyroscopeSensor
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.repository.LeanProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 상보 필터 전략(F-03): 자이로 roll-rate 를 적분해 단기 뱅킹을 추적하고, **직선 구간**에서만
 * 가속도 기반 roll 로 적분 드리프트를 보정한다. 정상선회(횡가속도 큼) 중에는 가속도 신뢰도를 낮춰
 * 융합 오염으로 인한 피크 lean 과소측정을 완화한다.
 *
 * 부호 규약: lean(roll)은 기기 Z축(화면 밖) 회전이며 [LeanMath.rollFromUpVector] 와 같은 부호.
 * 자이로 z축 각속도를 그대로 적분한다. (기기별 축/부호는 진단 로깅으로 검증 권장)
 */
@Singleton
class ComplementaryLeanProvider
    @Inject
    constructor(
        private val gyroSensor: GyroscopeSensor,
        private val accelSensor: AccelerometerSensor,
    ) : LeanProvider {
        override val mode = LeanMode.COMPLEMENTARY

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var job: Job? = null

        private val _leanStream = MutableStateFlow(LeanMath.NO_DATA)
        override val leanStream: StateFlow<Float> = _leanStream.asStateFlow()

        // 센서 콜백 수집 코루틴(Default)과 start/stop(다른 스레드)에서 접근하므로 가시성 보장.
        @Volatile
        private var fused = 0f

        @Volatile
        private var lastTimestampNanos = 0L

        @Volatile
        private var initialized = false

        override fun start() {
            if (job?.isActive == true) return
            fused = 0f
            lastTimestampNanos = 0L
            initialized = false
            gyroSensor.start()
            accelSensor.start()
            job =
                scope.launch {
                    gyroSensor.dataFlow.collect { gyro ->
                        if (gyro.timestampNanos == 0L) return@collect

                        val accel = accelSensor.dataFlow.value
                        val accelRoll =
                            if (accel.timestamp != 0L) LeanMath.rollFromUpVector(accel.x, accel.y, accel.z) else null

                        // 첫 유효 샘플: 가속도 roll 이 들어와야 초기화한다(그 전엔 데이터 없음).
                        if (!initialized) {
                            if (accelRoll == null) {
                                _leanStream.value = LeanMath.NO_DATA
                                return@collect
                            }
                            fused = accelRoll
                            lastTimestampNanos = gyro.timestampNanos
                            initialized = true
                            _leanStream.value = fused
                            return@collect
                        }

                        val dt = (gyro.timestampNanos - lastTimestampNanos) / NANOS_PER_SEC
                        lastTimestampNanos = gyro.timestampNanos
                        if (dt <= 0f || dt > MAX_DT_SEC) {
                            _leanStream.value = fused
                            return@collect
                        }

                        // 자이로 적분(고주파). z축 각속도(rad/s) → 도.
                        val gyroDeltaDeg = Math.toDegrees((gyro.z * dt).toDouble()).toFloat()
                        val predicted = fused + gyroDeltaDeg

                        fused =
                            if (accelRoll != null) {
                                val accelMag = sqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z)
                                // 횡가속도가 작을수록(직선) 가속도 신뢰 ↑ → drift 보정. 코너일수록 자이로 신뢰 ↑.
                                val straight = abs(accelMag - GRAVITY) < STRAIGHT_TOLERANCE
                                val alpha = if (straight) ALPHA_STRAIGHT else ALPHA_CORNER
                                alpha * predicted + (1f - alpha) * accelRoll
                            } else {
                                predicted
                            }
                        _leanStream.value = fused
                    }
                }
        }

        override fun stop() {
            job?.cancel()
            job = null
            gyroSensor.stop()
            accelSensor.stop()
            _leanStream.value = LeanMath.NO_DATA
            initialized = false
        }

        companion object {
            private const val NANOS_PER_SEC = 1_000_000_000f
            private const val GRAVITY = 9.81f

            /** |가속도 크기 − g| 가 이보다 작으면 직선(횡가속도 미미)으로 본다(m/s²). */
            private const val STRAIGHT_TOLERANCE = 0.6f

            /** 직선 구간: 가속도로 적극 보정(드리프트 억제). */
            private const val ALPHA_STRAIGHT = 0.95f

            /** 코너 구간: 자이로를 강하게 신뢰(오염된 가속도 영향 최소화). */
            private const val ALPHA_CORNER = 0.995f

            /** 샘플 끊김 시 과도 적분 방지 상한(초). */
            private const val MAX_DT_SEC = 0.25f
        }
    }
