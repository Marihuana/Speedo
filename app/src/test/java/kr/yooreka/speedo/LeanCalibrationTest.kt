package kr.yooreka.speedo

import kr.yooreka.speedo.data.sensor.repository.LeanCalibrationRepositoryImpl
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 약 30도 기울어진 중력 벡터 샘플. (x=5, z≈8.66 → 자기장 크기 ≈ 10)
 * roll = atan2(5, 8.66) ≈ 30°
 */
private val TILTED = GravityData(x = 5f, y = 0f, z = 8.66f)

/** 테스트용 가짜 중력 센서 리포지토리. */
private class FakeGravityRepository(
    initial: GravityData = GravityData()
) : SensorRepository<GravityData> {
    private val _data = MutableStateFlow(initial)
    override val dataStream: StateFlow<GravityData> = _data

    var startCount = 0
    var stopCount = 0

    /** start() 시 방출할 값(센서가 깨어나 첫 샘플을 내보내는 상황 모사). */
    var emitOnStart: GravityData? = null

    override fun start() {
        startCount++
        emitOnStart?.let { _data.value = it }
    }

    override fun stop() {
        stopCount++
        _data.value = GravityData() // 실제 GravitySensor.stop() 과 동일하게 (0,0,0) 초기화
    }
}

class LeanCalibrationTest {

    // ---------- 순수 로직: calibratedRoll ----------

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
        val repo = LeanCalibrationRepositoryImpl(FakeGravityRepository())
        assertEquals(0f, repo.offsetDegrees.value, 0.0f)
    }

    @Test
    fun `calibrate while sensor already streaming snapshots current roll without toggling sensor`() = runBlocking {
        val gravity = FakeGravityRepository(initial = TILTED) // 이미 스트리밍 중
        val repo = LeanCalibrationRepositoryImpl(gravity)

        repo.calibrate()

        assertEquals(TILTED.calculateRoll(), repo.offsetDegrees.value, 0.001f)
        assertEquals("이미 켜져 있으면 start() 호출 안 함", 0, gravity.startCount)
        assertEquals("이미 켜져 있으면 stop() 호출 안 함", 0, gravity.stopCount)
    }

    @Test
    fun `calibrate while sensor off starts samples then stops`() = runBlocking {
        val gravity = FakeGravityRepository(initial = GravityData()) // 꺼진 상태
        gravity.emitOnStart = TILTED
        val repo = LeanCalibrationRepositoryImpl(gravity)

        repo.calibrate()

        assertEquals(TILTED.calculateRoll(), repo.offsetDegrees.value, 0.001f)
        assertEquals("꺼져 있으면 start() 1회", 1, gravity.startCount)
        assertEquals("이 호출이 켰으므로 stop() 1회", 1, gravity.stopCount)
    }

    @Test
    fun `after calibration same tilt reads near zero lean`() = runBlocking {
        val gravity = FakeGravityRepository(initial = TILTED)
        val repo = LeanCalibrationRepositoryImpl(gravity)

        repo.calibrate()
        val offset = repo.offsetDegrees.value

        // 영점을 잡은 자세 그대로라면 보정 기울기는 0 근처
        assertTrue(kotlin.math.abs(TILTED.calibratedRoll(offset)) < 0.01f)
    }

    @Test
    fun `reset zeroes the offset`() = runBlocking {
        val gravity = FakeGravityRepository(initial = TILTED)
        val repo = LeanCalibrationRepositoryImpl(gravity)

        repo.calibrate()
        assertTrue(repo.offsetDegrees.value != 0f)

        repo.reset()
        assertEquals(0f, repo.offsetDegrees.value, 0.0f)
    }
}
