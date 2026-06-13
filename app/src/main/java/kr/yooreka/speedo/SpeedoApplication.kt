package kr.yooreka.speedo

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import javax.inject.Inject

@HiltAndroidApp
class SpeedoApplication : Application() {
    @Inject
    lateinit var telemetryRepository: TelemetryRepository

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) {}

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
