package kr.yooreka.speedo

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.service.OverlayCoordinator
import javax.inject.Inject

@HiltAndroidApp
class SpeedoApplication : Application() {
    @Inject
    lateinit var telemetryRepository: TelemetryRepository

    @Inject
    lateinit var overlayCoordinator: OverlayCoordinator

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) {}

        // 플로팅 오버레이(F-19) 표시 시점 제어 시작.
        overlayCoordinator.start()

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    // 앱이 백그라운드로 가거나 종료될 때 버퍼 강제 저장
                    telemetryRepository.flushBuffer()
                }
            },
        )
    }
}
