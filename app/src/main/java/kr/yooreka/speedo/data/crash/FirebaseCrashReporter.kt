package kr.yooreka.speedo.data.crash

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.yooreka.speedo.domain.repository.CrashReporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Crashlytics 기반 [CrashReporter] 구현(PRD §7.1).
 *
 * google-services.json 이 없는 로컬 개발 환경에서는 [FirebaseApp] 이 초기화되지 않으므로,
 * [FirebaseCrashlytics.getInstance] 호출 전에 초기화 여부를 확인해 안전하게 no-op 한다.
 * 수집 활성/비활성(debug 제외)은 [SpeedoApplication] 의 setCrashlyticsCollectionEnabled 정책을 따르며,
 * 비활성 상태에서 recordException 은 Crashlytics 내부적으로 무시된다.
 */
@Singleton
class FirebaseCrashReporter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CrashReporter {
        override fun recordNonFatal(
            throwable: Throwable,
            message: String?,
        ) {
            // Firebase 미초기화(google-services.json 부재) 시 즉시 no-op 로 반환해 크래시를 막는다.
            if (FirebaseApp.getApps(context).isEmpty()) return
            try {
                val crashlytics = FirebaseCrashlytics.getInstance()
                if (!message.isNullOrBlank()) crashlytics.log(message)
                crashlytics.recordException(throwable)
            } catch (e: Exception) {
                // 리포팅은 부가 기능이므로 Crashlytics 자체 실패가 호출부 동작을 깨지 않도록 방어(no-op).
            }
        }
    }
