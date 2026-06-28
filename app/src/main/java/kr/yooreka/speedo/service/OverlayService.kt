package kr.yooreka.speedo.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.OverlaySettings
import kr.yooreka.speedo.domain.usecase.GetDashboardTelemetryUseCase
import kr.yooreka.speedo.ui.MainActivity
import kr.yooreka.speedo.ui.theme.SpeedoTheme
import kr.yooreka.speedo.ui.widget.OverlayContent
import kr.yooreka.speedo.utils.displaySpeedInt
import kr.yooreka.speedo.utils.formatLeanAngle
import javax.inject.Inject

/**
 * 플로팅 오버레이 위젯을 시스템 윈도우로 렌더링하는 서비스(F-19).
 *
 * 표시/숨김 시점은 [OverlayCoordinator]가 제어한다(주행 중 + 백그라운드 + 설정 ON + 권한 허용).
 * ComposeView 를 [WindowManager]에 붙이기 위해 Lifecycle/ViewModelStore/SavedStateRegistry
 * 오너를 직접 구현하여 ViewTree 에 연결한다.
 */
@AndroidEntryPoint
class OverlayService :
    Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    @Inject
    lateinit var getDashboardTelemetryUseCase: GetDashboardTelemetryUseCase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private val layoutParams: WindowManager.LayoutParams by lazy { buildLayoutParams() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        addOverlayView()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // 표시/숨김은 코디네이터가 start/stopService 로 제어하므로 자동 재시작하지 않는다.
        return START_NOT_STICKY
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // minSdk 27 이므로 TYPE_APPLICATION_OVERLAY(API 26+) 상시 사용 가능.
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = INITIAL_X
            y = INITIAL_Y
        }

    private fun addOverlayView() {
        if (overlayView != null) return
        // 권한이 없으면(설정에서 회수 등) 오버레이를 띄우지 않고 서비스를 종료한다.
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val view =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@OverlayService)
                setViewTreeViewModelStoreOwner(this@OverlayService)
                setViewTreeSavedStateRegistryOwner(this@OverlayService)
                setContent {
                    SpeedoTheme {
                        // 표시값(정수 속도/기울기/제동/단위)으로 먼저 매핑하고 distinctUntilChanged 하여
                        // 화면에 보이는 값이 실제로 바뀔 때만 recomposition 한다(고주기 원시 스트림 낭비 방지).
                        val displayFlow =
                            remember {
                                combine(
                                    getDashboardTelemetryUseCase(),
                                    userPreferencesRepository.userPreferencesFlow
                                        .map { it.speedUnit }
                                        .distinctUntilChanged(),
                                ) { telemetry, unit ->
                                    OverlayDisplay(
                                        speed = displaySpeedInt(telemetry.speed, unit).toString(),
                                        leanAngle = formatLeanAngle(telemetry.roll),
                                        isHardBrake = telemetry.brakeEvent == BrakeEvent.HARD,
                                        speedUnit = unit,
                                    )
                                }.distinctUntilChanged()
                            }
                        val display by displayFlow.collectAsStateWithLifecycle(initialValue = OverlayDisplay())
                        val settings by remember { userPreferencesRepository.overlaySettingsFlow.distinctUntilChanged() }
                            .collectAsStateWithLifecycle(initialValue = OverlaySettings())

                        OverlayContent(
                            speedKmh = display.speed,
                            leanAngle = display.leanAngle,
                            isHardBrake = display.isHardBrake,
                            speedUnit = display.speedUnit,
                            mode = settings.mode,
                            size = settings.size,
                            opacity = settings.opacity,
                            onTap = ::bringAppToForeground,
                            onDrag = ::moveOverlay,
                        )
                    }
                }
            }

        overlayView = view
        runCatching { windowManager.addView(view, layoutParams) }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /** 위치 이동(F-19b): 롱클릭 드래그 픽셀 이동량을 윈도우 좌표에 반영한다. */
    private fun moveOverlay(
        dxPx: Float,
        dyPx: Float,
    ) {
        val view = overlayView ?: return
        layoutParams.x += dxPx.toInt()
        layoutParams.y += dyPx.toInt()
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    /** 위젯 탭 시 앱을 포그라운드로 복귀시키고 대시보드를 전면에 띄운다(F-19, AC-04). */
    private fun bringAppToForeground() {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
        startActivity(intent)
    }

    private fun removeOverlayView() {
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
    }

    override fun onDestroy() {
        // 현재 상태에서 DESTROYED 로의 하강 전이를 레지스트리가 알아서 올바른 순서로 디스패치한다.
        // (조기 stopSelf 로 CREATED 상태인 경우에도 잘못된 상향 전이가 발생하지 않도록)
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        removeOverlayView()
        store.clear()
        super.onDestroy()
    }

    companion object {
        private const val INITIAL_X = 24
        private const val INITIAL_Y = 200

        /**
         * 오버레이 서비스를 시작한다. 시작 성공 여부를 반환한다.
         * 백그라운드 서비스 시작 제약(Android 8+)으로 실패할 수 있어(타이밍 갭) 호출자가 재시도하도록
         * 성공/실패를 알린다.
         */
        fun start(context: Context): Boolean {
            val intent = Intent(context, OverlayService::class.java)
            return runCatching { context.startService(intent) }.isSuccess
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}

/** 오버레이에 실제로 렌더되는 표시값. 원시 텔레메트리를 정수/문자열로 미리 환산해 불필요한 recomposition을 줄인다. */
private data class OverlayDisplay(
    val speed: String = "0",
    val leanAngle: String = "0°",
    val isHardBrake: Boolean = false,
    val speedUnit: String = "KM/H",
)
