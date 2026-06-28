package kr.yooreka.speedo.data.sensor.lean

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import java.io.BufferedWriter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * lean 측정 방식 비교(F-03)를 위한 진단 로거. 코어 측정 시스템과 **완전히 독립**된 자체 센서
 * 리스너로 원시 5종 센서(가속도/중력/자이로/회전벡터/게임회전벡터)와 GPS 속도를 동일 timestamp 로
 * CSV(앱 전용 외부 저장소)에 기록한다. 오프라인에서 어느 방식이 실제 뱅킹에 가장 근접한지 분석한다.
 *
 * 인앱 실시간 비교는 설정의 측정 방식 선택자로, 본 로거는 객관적 사후 분석용으로 역할을 분리한다.
 */
@Singleton
class LeanDiagnosticLogger
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val locationRepo: SensorRepository<LocationData>,
        private val calibrationRepository: LeanCalibrationRepository,
    ) : SensorEventListener {
        private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var job: Job? = null
        private var writer: BufferedWriter? = null

        // 최신 원시 센서 값(센서 스레드에서 갱신, 타이머 스레드에서 읽음 — 진단용이라 느슨한 동기화 허용).
        private val accel = FloatArray(3)
        private val gravity = FloatArray(3)
        private val gyro = FloatArray(3)
        private val rotationVector = FloatArray(4)
        private val gameRotationVector = FloatArray(4)
        private val rotationMatrix = FloatArray(9)

        @Synchronized
        fun start() {
            if (job != null) return
            val dir = File(context.getExternalFilesDir(null), LOG_DIR).apply { mkdirs() }
            val file = File(dir, "lean_${System.currentTimeMillis()}.csv")
            writer = file.bufferedWriter().apply { appendLine(CSV_HEADER) }

            SENSOR_TYPES.forEach { type ->
                sensorManager.getDefaultSensor(type)?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
            }

            job =
                scope.launch {
                    while (isActive) {
                        delay(LOG_INTERVAL_MS)
                        writeRow()
                    }
                }
        }

        @Synchronized
        fun stop() {
            job?.cancel()
            job = null
            sensorManager.unregisterListener(this)
            runCatching {
                writer?.flush()
                writer?.close()
            }
            writer = null
        }

        /** 저장된 진단 CSV 파일 목록(최신순). Export(메일 전송)용. */
        fun logFiles(): List<File> {
            val dir = File(context.getExternalFilesDir(null), LOG_DIR)
            return dir.listFiles { f -> f.isFile && f.extension == "csv" }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        private fun writeRow() {
            val w = writer ?: return
            val speed = locationRepo.dataStream.value.speed
            val offset = calibrationRepository.offsetDegrees.value
            val gravityLean = LeanMath.rollFromUpVector(gravity[0], gravity[1], gravity[2])
            val accelLean = LeanMath.rollFromUpVector(accel[0], accel[1], accel[2])
            val rotationLean = rotationVectorLean(rotationVector)
            val gameRotationLean = rotationVectorLean(gameRotationVector)

            val row =
                listOf(
                    System.currentTimeMillis(), speed, offset,
                    gravityLean, accelLean, rotationLean, gameRotationLean,
                    accel[0], accel[1], accel[2],
                    gravity[0], gravity[1], gravity[2],
                    gyro[0], gyro[1], gyro[2],
                    rotationVector[0], rotationVector[1], rotationVector[2], rotationVector[3],
                    gameRotationVector[0], gameRotationVector[1], gameRotationVector[2], gameRotationVector[3],
                ).joinToString(",")
            runCatching { w.appendLine(row) }
        }

        private fun rotationVectorLean(v: FloatArray): Float {
            if (v[0] == 0f && v[1] == 0f && v[2] == 0f && v[3] == 0f) return LeanMath.NO_DATA
            val vector = if (v[3] != 0f) v else floatArrayOf(v[0], v[1], v[2])
            SensorManager.getRotationMatrixFromVector(rotationMatrix, vector)
            return LeanMath.rollFromUpVector(rotationMatrix[6], rotationMatrix[7], rotationMatrix[8])
        }

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, accel, 0, 3)
                Sensor.TYPE_GRAVITY -> System.arraycopy(event.values, 0, gravity, 0, 3)
                Sensor.TYPE_GYROSCOPE -> System.arraycopy(event.values, 0, gyro, 0, 3)
                Sensor.TYPE_ROTATION_VECTOR -> copyRotationVector(event.values, rotationVector)
                Sensor.TYPE_GAME_ROTATION_VECTOR -> copyRotationVector(event.values, gameRotationVector)
            }
        }

        private fun copyRotationVector(
            src: FloatArray,
            dst: FloatArray,
        ) {
            dst[0] = src[0]
            dst[1] = src[1]
            dst[2] = src[2]
            dst[3] = if (src.size >= 4) src[3] else 0f
        }

        override fun onAccuracyChanged(
            sensor: Sensor?,
            accuracy: Int,
        ) = Unit

        companion object {
            /** 진단 CSV 저장 디렉터리(앱 전용 외부 저장소 하위). */
            const val LOG_DIR = "lean_diag"

            private const val LOG_INTERVAL_MS = 100L

            private val SENSOR_TYPES =
                listOf(
                    Sensor.TYPE_ACCELEROMETER,
                    Sensor.TYPE_GRAVITY,
                    Sensor.TYPE_GYROSCOPE,
                    Sensor.TYPE_ROTATION_VECTOR,
                    Sensor.TYPE_GAME_ROTATION_VECTOR,
                )

            private const val CSV_HEADER =
                "timestamp_ms,gps_speed_kmh,offset_deg," +
                    "gravity_lean,accel_lean,rotvec_lean,gamerotvec_lean," +
                    "accel_x,accel_y,accel_z," +
                    "gravity_x,gravity_y,gravity_z," +
                    "gyro_x,gyro_y,gyro_z," +
                    "rotvec_x,rotvec_y,rotvec_z,rotvec_w," +
                    "gamerotvec_x,gamerotvec_y,gamerotvec_z,gamerotvec_w"
        }
    }
