package kr.yooreka.speedo

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
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

        // Crashlytics: google-services.json 으로 Firebase 가 초기화된 경우에만 설정(파일 없으면 no-op).
        // debug 빌드의 크래시는 수집에서 제외해 실사용자 데이터만 리포트한다.
        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }

        // 광고 초기화. 광고 단위 ID 는 debug=구글 테스트 ID, release=프로덕션 ID 로 분기한다(BannerAd/AdManager).
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
