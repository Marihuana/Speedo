package kr.yooreka.speedo.data.sensor.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @Singleton 으로 관리되며 offset 을 DataStore 에 영속화한다(F-04).
 *
 * 영점은 **활성 측정 전략(F-03)** 의 raw roll 을 기준으로 잡는다. 소비처는 `raw − offset` 으로
 * 보정하므로, 측정 방식을 바꿔도 영점 의미가 일관된다. 프로세스 킬 후에도 저장값을 하이드레이트하여
 * 사용자가 수동 초기화(reset)하기 전까지 값을 보존한다.
 */
@Singleton
class LeanCalibrationRepositoryImpl
    @Inject
    constructor(
        private val leanMeasurement: LeanMeasurement,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : LeanCalibrationRepository {
        // reset() 은 non-suspend 이고 앱 시작 시 저장값 하이드레이트가 필요하므로 Singleton 수명의 IO 스코프를 둔다.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val _offsetDegrees = MutableStateFlow(0f)
        override val offsetDegrees: StateFlow<Float> = _offsetDegrees.asStateFlow()

        // 사용자가 calibrate/reset 을 한 번이라도 눌렀는지. 하이드레이트가 사용자의 의도적 값(0f 포함)을
        // 저장값으로 되돌리는 레이스를 막는다.
        private val userInteracted = AtomicBoolean(false)

        init {
            // 저장된 오프셋을 로드해 하이드레이트. 로드 전 짧은 0f 윈도우는 허용한다.
            // 하이드레이트 도중 사용자가 이미 보정/초기화했다면 저장값으로 덮어쓰지 않는다.
            scope.launch {
                val stored = userPreferencesRepository.leanOffsetDegreesFlow.first()
                if (!userInteracted.get()) _offsetDegrees.compareAndSet(0f, stored)
            }
        }

        override suspend fun calibrate() {
            userInteracted.set(true)
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
                // 인메모리 갱신과 함께 영속 저장(프로세스 킬 대비).
                userPreferencesRepository.updateLeanOffset(sample)
            }

            if (startedByUs) leanMeasurement.stop()
        }

        override fun reset() {
            userInteracted.set(true)
            _offsetDegrees.value = 0f
            // 영속 저장도 0f 로 클리어(사용자 수동 초기화).
            scope.launch { userPreferencesRepository.updateLeanOffset(0f) }
        }

        companion object {
            private const val WARMUP_TIMEOUT_MS = 2000L
        }
    }
