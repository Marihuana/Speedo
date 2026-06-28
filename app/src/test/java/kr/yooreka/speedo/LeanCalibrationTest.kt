package kr.yooreka.speedo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kr.yooreka.speedo.data.sensor.repository.LeanCalibrationRepositoryImpl
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 약 30도 기울어진 중력 벡터 샘플. (x=5, z≈8.66 → 크기 ≈ 10)
 * roll = atan2(5, 8.66) ≈ 30°
 */
private val TILTED = GravityData(x = 5f, y = 0f, z = 8.66f)

/** TILTED 자세에서 측정 전략이 내보내는 raw roll(도). */
private val TILTED_ROLL = TILTED.calculateRoll()

/**
 * 테스트용 가짜 lean 측정자. 활성 전략의 raw roll 스트림을 모사한다.
 * 데이터 없음은 NaN, 정지 시 NaN 으로 초기화(실제 LeanProviderSelector 와 동일).
 */
private class FakeLeanMeasurement(
    initial: Float = Float.NaN,
    startedInitially: Boolean = false,
) : LeanMeasurement {
    private val _leanStream = MutableStateFlow(initial)
    override val leanStream: StateFlow<Float> = _leanStream

    private var started = startedInitially
    override val isStarted: Boolean get() = started

    var startCount = 0
    var stopCount = 0

    /** start() 시 방출할 값(전략이 깨어나 첫 샘플을 내보내는 상황 모사). */
    var emitOnStart: Float? = null

    override fun start() {
        startCount++
        started = true
        emitOnStart?.let { _leanStream.value = it }
    }

    override fun stop() {
        stopCount++
        started = false
        _leanStream.value = Float.NaN
    }
}

class LeanCalibrationTest {
    // ---------- 순수 로직: GravityData.calibratedRoll ----------

    @Test
    fun `calibratedRoll subtracts offset`() {
        val roll = TILTED.calculateRoll()
        // 영점을 같은 기울기로 잡으면 보정값은 0에 수렴한다.
        assertEquals(0f, TILTED.calibratedRoll(roll), 0.001f)
        // 영점이 10도라면 보정값은 (원래 roll - 10)
        assertEquals(roll - 10f, TILTED.calibratedRoll(10f), 0.001f)
    }

    @Test
    fun `calibratedRoll returns zero when sensor has no data`() {
        // 센서 미동작(0,0,0) 시 offset 이 있어도 -offset 이 아니라 0 을 반환해야 한다.
        val noData = GravityData()
        assertEquals(0f, noData.calibratedRoll(10f), 0.0f)
        assertEquals(0f, noData.calibratedRoll(-25f), 0.0f)
    }

    // ---------- 리포지토리: calibrate / reset ----------

    @Test
    fun `default offset is zero`() {
        val repo = LeanCalibrationRepositoryImpl(FakeLeanMeasurement())
        assertEquals(0f, repo.offsetDegrees.value, 0.0f)
    }

    @Test
    fun `calibrate while already running snapshots current lean without toggling`() =
        runBlocking {
            // 이미 구동 중이고 현재 raw roll 을 방출하고 있음
            val lean = FakeLeanMeasurement(initial = TILTED_ROLL, startedInitially = true)
            val repo = LeanCalibrationRepositoryImpl(lean)

            repo.calibrate()

            assertEquals(TILTED_ROLL, repo.offsetDegrees.value, 0.001f)
            assertEquals("이미 켜져 있으면 start() 호출 안 함", 0, lean.startCount)
            assertEquals("이미 켜져 있으면 stop() 호출 안 함", 0, lean.stopCount)
        }

    @Test
    fun `calibrate while off starts samples then stops`() =
        runBlocking {
            val lean = FakeLeanMeasurement(initial = Float.NaN, startedInitially = false)
            lean.emitOnStart = TILTED_ROLL
            val repo = LeanCalibrationRepositoryImpl(lean)

            repo.calibrate()

            assertEquals(TILTED_ROLL, repo.offsetDegrees.value, 0.001f)
            assertEquals("꺼져 있으면 start() 1회", 1, lean.startCount)
            assertEquals("이 호출이 켰으므로 stop() 1회", 1, lean.stopCount)
        }

    @Test
    fun `after calibration same tilt reads near zero lean`() =
        runBlocking {
            val lean = FakeLeanMeasurement(initial = TILTED_ROLL, startedInitially = true)
            val repo = LeanCalibrationRepositoryImpl(lean)

            repo.calibrate()
            val offset = repo.offsetDegrees.value

            // 영점을 잡은 자세 그대로라면 (raw - offset) 은 0 근처
            assertTrue(kotlin.math.abs(TILTED_ROLL - offset) < 0.01f)
        }

    @Test
    fun `reset zeroes the offset`() =
        runBlocking {
            val lean = FakeLeanMeasurement(initial = TILTED_ROLL, startedInitially = true)
            val repo = LeanCalibrationRepositoryImpl(lean)

            repo.calibrate()
            assertTrue(repo.offsetDegrees.value != 0f)

            repo.reset()
            assertEquals(0f, repo.offsetDegrees.value, 0.0f)
        }
}
