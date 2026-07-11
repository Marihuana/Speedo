package kr.yooreka.speedo.domain.model

/**
 * 광고 제거 구독 플랜(월구독 전환). 월간/연간 base plan과 무료체험 offer를 표현한다.
 *
 * 가격(formattedPrice)·무료체험 기간은 Play Console의 base plan/offer 설정에서 내려오며,
 * 결제 시 [offerToken] 으로 해당 플랜을 지정한다.
 */
data class SubscriptionPlan(
    val type: PlanType,
    val productId: String,
    /** 결제 플로우 시작 시 지정하는 offer 토큰(특정 base plan/offer 식별). */
    val offerToken: String,
    /** 정기 결제 가격(통화 기호 포함, Play 제공 포맷). */
    val formattedPrice: String,
    /** 정기 결제 금액(마이크로 단위, 1_000_000 = 1통화단위). 월환산·할인율 계산에 사용. */
    val priceAmountMicros: Long,
    /** ISO 4217 통화 코드(예: KRW, USD). 통화 포맷팅에 사용. */
    val priceCurrencyCode: String,
    /** 무료체험 제공 여부. */
    val hasFreeTrial: Boolean,
    /** 무료체험 기간(ISO 8601 duration, 예: P7D). 미제공 시 null. */
    val freeTrialPeriod: String? = null,
) {
    enum class PlanType { MONTHLY, YEARLY }
}
