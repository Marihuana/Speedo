package kr.yooreka.speedo.data.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * 인앱 결제(광고 제거) 상태를 노출하고 결제 플로우를 시작하는 레포지토리.
 *
 * ViewModel은 이 인터페이스에만 의존하여(DIP) Play Billing 구현([BillingRepositoryImpl])과 분리되고
 * 테스트에서 Fake로 대체할 수 있다.
 */
interface BillingRepository {
    /** 광고 제거(프리미엄) 구매 여부. */
    val isAdRemoved: StateFlow<Boolean>

    /** 광고 제거 상품 결제 플로우를 시작한다. */
    fun launchBillingFlow(activity: Activity)
}
