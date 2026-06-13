package kr.yooreka.speedo.data.sensor.repository

import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @Singleton 으로 관리되어 offset 이 앱 종료 시까지 유지된다.
 */
@Singleton
class LeanCalibrationRepositoryImpl @Inject constructor(
    private val gravityRepo: SensorRepository<GravityData>
) : LeanCalibrationRepository {

    private val _offsetDegrees = MutableStateFlow(0f)
    override val offsetDegrees: StateFlow<Float> = _offsetDegrees.asStateFlow()

    override suspend fun calibrate() {
        // 센서가 이미 스트리밍 중인지(=다른 화면에서 사용 중인지) 판단.
        // stop() 시 (0,0,0)으로 초기화되므로 hasData()==false 면 꺼진 상태로 간주한다.
        val alreadyStreaming = gravityRepo.dataStream.value.hasData()
        if (!alreadyStreaming) {
            gravityRepo.start()
            // 첫 유효 샘플이 도착할 때까지 대기(최대 타임아웃). StateFlow.first 는
            // 현재 값이 조건을 만족하면 즉시 반환한다.
            withTimeoutOrNull(WARMUP_TIMEOUT_MS) {
                gravityRepo.dataStream.first { it.hasData() }
            }
        }

        val snapshot = gravityRepo.dataStream.value
        if (snapshot.hasData()) {
            _offsetDegrees.value = snapshot.calculateRoll()
        }
        // 무효 데이터(타임아웃 등)이면 기존 offset 을 유지한다.

        // 이 호출이 켠 경우에만 다시 끈다(다른 화면 사용 중이면 건드리지 않음).
        if (!alreadyStreaming) {
            gravityRepo.stop()
        }
    }

    override fun reset() {
        _offsetDegrees.value = 0f
    }

    companion object {
        private const val WARMUP_TIMEOUT_MS = 2000L
    }
}
