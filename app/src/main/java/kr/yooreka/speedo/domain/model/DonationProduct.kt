package kr.yooreka.speedo.domain.model

/**
 * 개발자 후원(일회성 인앱 결제) 상품. "개발자에게 오토바이 사주기" 버튼용.
 *
 * 광고 제거 구독([SubscriptionPlan])과 달리 권한(entitlement)을 부여하지 않고 소비(consume)되어
 * 반복 결제가 가능하다. Play Console 에 소비성(consumable) 인앱 상품으로 등록되어야 노출된다.
 */
data class DonationProduct(
    val productId: String,
    /** Play 가 지역/통화에 맞춰 포맷한 가격 문자열(예: "₩5,000"). */
    val formattedPrice: String,
)
