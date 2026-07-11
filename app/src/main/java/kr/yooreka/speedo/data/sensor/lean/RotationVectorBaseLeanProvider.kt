package kr.yooreka.speedo.data.sensor.lean

import android.content.Context
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
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
    private val context: Context,
    private val sensor: SensorDataSource<RotationVectorData>,
) : LeanProvider {
    // 앱(Application) 컨텍스트에서도 안전하게 화면 방향을 얻기 위해 DisplayManager 를 사용한다.
    // Context.getDisplay() 는 디스플레이 미연결(Application) 컨텍스트에서 UnsupportedOperationException 을 던지므로 쓰지 않는다.
    private val displayManager by lazy {
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private fun currentRotation(): Int =
        try {
            displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        } catch (e: Exception) {
            Surface.ROTATION_0
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val rotationMatrix = FloatArray(9)

    // 고주파 센서 이벤트마다 새 배열을 만들지 않도록 입력 버퍼를 재사용한다(단일 수집 코루틴 전용).
    private val vector4 = FloatArray(4)
    private val vector3 = FloatArray(3)

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
                    val vector =
                        if (d.w != 0f) {
                            vector4[0] = d.x
                            vector4[1] = d.y
                            vector4[2] = d.z
                            vector4[3] = d.w
                            vector4
                        } else {
                            vector3[0] = d.x
                            vector3[1] = d.y
                            vector3[2] = d.z
                            vector3
                        }
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, vector)

                    val rotation = currentRotation()

                    val remappedMatrix = FloatArray(9)
                    when (rotation) {
                        Surface.ROTATION_90 -> {
                            SensorManager.remapCoordinateSystem(
                                rotationMatrix,
                                SensorManager.AXIS_Y,
                                SensorManager.AXIS_MINUS_X,
                                remappedMatrix,
                            )
                        }
                        Surface.ROTATION_180 -> {
                            SensorManager.remapCoordinateSystem(
                                rotationMatrix,
                                SensorManager.AXIS_MINUS_X,
                                SensorManager.AXIS_MINUS_Y,
                                remappedMatrix,
                            )
                        }
                        Surface.ROTATION_270 -> {
                            SensorManager.remapCoordinateSystem(
                                rotationMatrix,
                                SensorManager.AXIS_MINUS_Y,
                                SensorManager.AXIS_X,
                                remappedMatrix,
                            )
                        }
                        else -> {
                            System.arraycopy(rotationMatrix, 0, remappedMatrix, 0, 9)
                        }
                    }

                    _leanStream.value = LeanMath.rollFromUpVector(remappedMatrix[6], remappedMatrix[7], remappedMatrix[8])
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
