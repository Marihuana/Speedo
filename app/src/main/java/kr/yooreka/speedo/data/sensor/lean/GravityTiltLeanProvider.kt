package kr.yooreka.speedo.data.sensor.lean

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.sensor.datasource.GravitySensor
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.repository.LeanProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현행 기본 전략(F-03): TYPE_GRAVITY + atan2. 정상선회 시 센서융합이 원심가속도를 중력으로
 * 오인하여 피크 lean 을 과소 측정할 수 있다(비교 기준선).
 */
@Singleton
class GravityTiltLeanProvider
    @Inject
    constructor(
        private val gravitySensor: GravitySensor,
    ) : LeanProvider {
        override val mode = LeanMode.GRAVITY_TILT

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var job: Job? = null

        private val _leanStream = MutableStateFlow(LeanMath.NO_DATA)
        override val leanStream: StateFlow<Float> = _leanStream.asStateFlow()

        override fun start() {
            if (job?.isActive == true) return
            gravitySensor.start()
            job =
                scope.launch {
                    gravitySensor.dataFlow.collect { g ->
                        _leanStream.value =
                            if (g.hasData()) LeanMath.rollFromUpVector(g.x, g.y, g.z) else LeanMath.NO_DATA
                    }
                }
        }

        override fun stop() {
            job?.cancel()
            job = null
            gravitySensor.stop()
            _leanStream.value = LeanMath.NO_DATA
        }
    }
