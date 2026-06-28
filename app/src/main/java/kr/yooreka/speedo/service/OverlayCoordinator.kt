package kr.yooreka.speedo.service

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 플로팅 오버레이(F-19) 표시 시점을 제어하는 컨트롤러.
 *
 * 표시 조건: **주행 기록 중** + **앱이 백그라운드** + **오버레이 설정 ON** + **오버레이 권한 허용**.
 * UI/ViewModel 이 직접 서비스를 시작하지 않고 이 컨트롤러가 [OverlayService] 를 start/stop 하여
 * 계층 분리를 유지한다(service_layer 표준). 주행 중에는 [RecordingService] 포그라운드가 떠 있어
 * 백그라운드에서의 서비스 시작이 허용된다.
 */
@Singleton
class OverlayCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val telemetryRepository: TelemetryRepository,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) {
        // ProcessLifecycleOwner 관찰/서비스 제어는 메인 스레드에서 수행한다.
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val isForeground = MutableStateFlow(true)
        private var shown = false

        /** Application.onCreate 에서 1회 호출한다. */
        fun start() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        isForeground.value = true
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        isForeground.value = false
                    }
                },
            )

            scope.launch {
                // distinctUntilChanged 를 두지 않는다: 권한(canDrawOverlays)은 Flow 가 아니므로
                // 매 입력(포그라운드/기록/설정) emission 마다 재평가해 최신 권한 상태를 반영한다.
                combine(
                    isForeground,
                    telemetryRepository.isRecording,
                    userPreferencesRepository.overlaySettingsFlow,
                ) { foreground, recording, settings ->
                    !foreground && recording && settings.enabled
                }.collect { eligible ->
                    updateOverlay(eligible)
                }
            }
        }

        private fun updateOverlay(eligible: Boolean) {
            // 표시 시점마다 권한을 새로 확인한다(설정 화면에서 권한이 회수된 경우 즉시 반영).
            val shouldShow = eligible && Settings.canDrawOverlays(context)
            when {
                shouldShow && !shown -> {
                    // 시작이 실패(백그라운드 제약 등)하면 shown 을 false 로 유지해 다음 emission 에서 재시도한다.
                    shown = OverlayService.start(context)
                }
                !shouldShow && shown -> {
                    OverlayService.stop(context)
                    shown = false
                }
            }
        }
    }
