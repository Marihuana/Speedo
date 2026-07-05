package kr.yooreka.speedo.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow
import kr.yooreka.speedo.domain.model.DonationProduct
import kr.yooreka.speedo.domain.model.SubscriptionPlan

/**
 * 인앱 결제(광고 제거) 상태를 노출하고 결제 플로우를 시작하는 레포지토리.
 *
 * 광고 제거는 월구독(SUBS, 월간/연간/무료체험)으로 제공된다.
 * 소비처(ViewModel)는 이 도메인 인터페이스에만 의존(DIP)하여 Play Billing 구현
 * ([kr.yooreka.speedo.data.billing.BillingRepositoryImpl])과 분리되고 테스트에서 Fake로 대체된다.
 */
interface BillingRepository {
    /** 광고 제거(프리미엄) 구독 활성 여부. */
    val isAdRemoved: StateFlow<Boolean>

    /** 구매 가능한 구독 플랜(월간/연간). 상품 조회 전엔 빈 리스트. */
    val subscriptionPlans: StateFlow<List<SubscriptionPlan>>

    /** 개발자 후원(일회성 인앱) 상품. 상품 조회 전이거나 미등록이면 null. */
    val donationProduct: StateFlow<DonationProduct?>

    /** 선택한 구독 플랜의 결제 플로우를 시작한다. (결제 UI 특성상 Activity 필요) */
    fun launchBillingFlow(
        activity: Activity,
        plan: SubscriptionPlan,
    )

    /** 개발자 후원(일회성) 결제 플로우를 시작한다. */
    fun launchDonation(activity: Activity)
}
