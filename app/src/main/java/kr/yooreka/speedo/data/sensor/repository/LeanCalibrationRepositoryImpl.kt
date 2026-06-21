package kr.yooreka.speedo.data.sensor.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @Singleton 으로 관리되어 offset 이 앱 종료 시까지 유지된다.
 *
 * 영점은 **활성 측정 전략(F-03)** 의 raw roll 을 기준으로 잡는다. 소비처는 `raw − offset` 으로
 * 보정하므로, 측정 방식을 바꿔도 영점 의미가 일관된다.
 */
@Singleton
class LeanCalibrationRepositoryImpl
    @Inject
    constructor(
        private val leanMeasurement: LeanMeasurement,
    ) : LeanCalibrationRepository {
        private val _offsetDegrees = MutableStateFlow(0f)
        override val offsetDegrees: StateFlow<Float> = _offsetDegrees.asStateFlow()

        override suspend fun calibrate() {
            // 이 호출이 선택자를 켠 경우에만 다시 끈다(대시보드 등에서 사용 중이면 건드리지 않음).
            val startedByUs = !leanMeasurement.isStarted
            if (startedByUs) leanMeasurement.start()

            // 첫 유효 샘플(NaN 아님)이 도착할 때까지 대기(최대 타임아웃).
            val sample =
                withTimeoutOrNull(WARMUP_TIMEOUT_MS) {
                    leanMeasurement.leanStream.first { !it.isNaN() }
                }
            // 유효 샘플의 raw roll 을 offset 으로. 무효(타임아웃 등)이면 기존 offset 유지.
            if (sample != null) {
                _offsetDegrees.value = sample
            }

            if (startedByUs) leanMeasurement.stop()
        }

        override fun reset() {
            _offsetDegrees.value = 0f
        }

        companion object {
            private const val WARMUP_TIMEOUT_MS = 2000L
        }
    }
