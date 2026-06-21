package kr.yooreka.speedo.data.sensor.lean

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import kr.yooreka.speedo.domain.repository.LeanProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 설정값([UserPreferencesRepository.leanMeasurementModeFlow])에 따라 활성 [LeanProvider] 전략을
 * 런타임에 교체하고, 활성 전략의 lean(부호 있는 raw roll, 도)을 [leanStream] 으로 단일 노출한다(F-03).
 *
 * 소비처(대시보드/기록/영점보정)는 측정 방식을 몰라도 되며 이 [leanStream] 에만 의존한다.
 * 활성 전략 하나만 구동하여 불필요한 센서 가동을 피한다(진단 동시 로깅은 별도 기능).
 */
@Singleton
class LeanProviderSelector
    @Inject
    constructor(
        providers: Set<@JvmSuppressWildcards LeanProvider>,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : LeanMeasurement {
        private val byMode: Map<LeanMode, LeanProvider> = providers.associateBy { it.mode }

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var modeJob: Job? = null
        private var streamJob: Job? = null
        private var active: LeanProvider? = null
        private var started = false

        private val _leanStream = MutableStateFlow(LeanMath.NO_DATA)
        override val leanStream: StateFlow<Float> = _leanStream.asStateFlow()

        /** 진단 로깅(다중 전략 동시 비교)용 전체 전략 맵. */
        val providersByMode: Map<LeanMode, LeanProvider> get() = byMode

        /** 이미 구동 중인지. 영점 보정이 자신이 켠 경우에만 끄도록 판단할 때 쓴다. */
        override val isStarted: Boolean
            @Synchronized get() = started

        @Synchronized
        override fun start() {
            if (started) return
            started = true
            modeJob =
                scope.launch {
                    userPreferencesRepository.leanMeasurementModeFlow.collect { mode -> switchTo(mode) }
                }
        }

        @Synchronized
        override fun stop() {
            if (!started) return
            started = false
            modeJob?.cancel()
            modeJob = null
            streamJob?.cancel()
            streamJob = null
            active?.stop()
            active = null
            _leanStream.value = LeanMath.NO_DATA
        }

        @Synchronized
        private fun switchTo(mode: LeanMode) {
            if (!started) return
            val next = byMode[mode] ?: byMode[LeanMode.DEFAULT] ?: return
            if (next === active) return
            active?.stop()
            streamJob?.cancel()
            active = next
            next.start()
            streamJob =
                scope.launch {
                    next.leanStream.collect { _leanStream.value = it }
                }
        }
    }
