package kr.yooreka.speedo.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * 인앱 결제(광고 제거) 상태를 노출하고 결제 플로우를 시작하는 레포지토리.
 *
 * 소비처(ViewModel)는 이 도메인 인터페이스에만 의존(DIP)하여 Play Billing 구현
 * ([kr.yooreka.speedo.data.billing.BillingRepositoryImpl])과 분리되고 테스트에서 Fake로 대체된다.
 */
interface BillingRepository {
    /** 광고 제거(프리미엄) 구매 여부. */
    val isAdRemoved: StateFlow<Boolean>

    /** 광고 제거 상품 결제 플로우를 시작한다. (결제 UI 특성상 Activity 필요) */
    fun launchBillingFlow(activity: Activity)
}
