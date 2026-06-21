package kr.yooreka.speedo.data.sensor.lean

import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.sensor.datasource.SensorDataSource
import kr.yooreka.speedo.domain.model.RotationVectorData
import kr.yooreka.speedo.domain.repository.LeanProvider

/**
 * 회전벡터(쿼터니언) 기반 lean 전략의 공통 구현. 회전행렬을 구해 기기 좌표계의 '위(up)' 방향
 * (행렬 3번째 행)을 추출하고, 다른 전략과 동일한 부호 규약으로 roll 을 산출한다.
 * 구체 센서(일반/게임 회전벡터)는 하위 클래스가 주입한다.
 */
abstract class RotationVectorBaseLeanProvider(
    private val sensor: SensorDataSource<RotationVectorData>,
) : LeanProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val rotationMatrix = FloatArray(9)

    private val _leanStream = MutableStateFlow(LeanMath.NO_DATA)
    override val leanStream: StateFlow<Float> = _leanStream.asStateFlow()

    override fun start() {
        if (job?.isActive == true) return
        sensor.start()
        job =
            scope.launch {
                sensor.dataFlow.collect { d ->
                    if (!d.hasData) {
                        _leanStream.value = LeanMath.NO_DATA
                        return@collect
                    }
                    // w 미제공(0) 기기는 length-3 으로 넘겨 w 를 내부 계산하게 한다.
                    val vector = if (d.w != 0f) floatArrayOf(d.x, d.y, d.z, d.w) else floatArrayOf(d.x, d.y, d.z)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, vector)
                    _leanStream.value = LeanMath.rollFromUpVector(rotationMatrix[6], rotationMatrix[7], rotationMatrix[8])
                }
            }
    }

    override fun stop() {
        job?.cancel()
        job = null
        sensor.stop()
        _leanStream.value = LeanMath.NO_DATA
    }
}
