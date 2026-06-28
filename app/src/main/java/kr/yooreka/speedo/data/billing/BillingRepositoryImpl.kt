package kr.yooreka.speedo.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.domain.model.SubscriptionPlan
import kr.yooreka.speedo.domain.repository.BillingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BillingRepository, PurchasesUpdatedListener, BillingClientStateListener {
        companion object {
            // Dev Testing Flag: Set to true to mock "Ads Removed" state globally
            const val FORCE_ADS_OFF = false

            // 광고 제거 구독 상품 ID(Play Console과 일치 필요). 월간/연간은 base plan ID가 아니라
            // 결제 주기(billingPeriod)로 판별해 콘솔 네이밍에 의존하지 않는다.
            private const val SUBSCRIPTION_PRODUCT_ID = "remove_ads_subscription"
        }

        // @Singleton 수명과 함께 사는 구조적 스코프(즉석 CoroutineScope 생성 금지).
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val _isAdRemoved = MutableStateFlow(FORCE_ADS_OFF)
        override val isAdRemoved: StateFlow<Boolean> = _isAdRemoved.asStateFlow()

        private val _subscriptionPlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
        override val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = _subscriptionPlans.asStateFlow()

        private val billingClient: BillingClient =
            BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()

        private var productDetails: ProductDetails? = null

        init {
            if (!FORCE_ADS_OFF) {
                connectToPlayBilling()
            }
        }

        private fun connectToPlayBilling() {
            billingClient.startConnection(this)
        }

        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryPurchases()
                queryProductDetails()
            }
        }

        override fun onBillingServiceDisconnected() {
            // Try to restart the connection on the next request to
            // Google Play by calling the startConnection() method.
        }

        private fun queryPurchases() {
            if (!billingClient.isReady) return

            val params =
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 활성 구독 전체 집합을 권위 있는 소스로 삼아 광고 제거 상태를 양방향 갱신한다.
                    updateAdRemovedState(purchases)
                    acknowledgePurchases(purchases)
                }
            }
        }

        private fun queryProductDetails() {
            val queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(SUBSCRIPTION_PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build(),
                        ),
                    )
                    .build()

            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    val details = productDetailsList.first()
                    productDetails = details
                    _subscriptionPlans.value = buildPlans(details)
                }
            }
        }

        /**
         * 구독 상품의 offer 목록을 월간/연간 플랜으로 변환한다.
         * - 가격: 0원이 아닌 마지막 정기 결제 단계의 포맷 가격.
         * - 무료체험: 0원 단계(있으면)의 기간.
         * - 동일 base plan에 offer가 여러 개면 무료체험 포함 offer를 우선한다.
         */
        private fun buildPlans(details: ProductDetails): List<SubscriptionPlan> {
            val offers = details.subscriptionOfferDetails ?: return emptyList()

            val plans =
                offers.mapNotNull { offer ->
                    val phases = offer.pricingPhases.pricingPhaseList
                    val recurring =
                        phases.lastOrNull { it.priceAmountMicros > 0L }
                            ?: phases.lastOrNull()
                            ?: return@mapNotNull null
                    val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
                    // 결제 주기로 월간/연간 판별(P1Y/P52W/P12M 등 1년 이상 → 연간).
                    val type =
                        if (isYearlyPeriod(recurring.billingPeriod)) {
                            SubscriptionPlan.PlanType.YEARLY
                        } else {
                            SubscriptionPlan.PlanType.MONTHLY
                        }

                    SubscriptionPlan(
                        type = type,
                        productId = details.productId,
                        offerToken = offer.offerToken,
                        formattedPrice = recurring.formattedPrice,
                        hasFreeTrial = trial != null,
                        freeTrialPeriod = trial?.billingPeriod,
                    )
                }

            return plans
                .groupBy { it.type }
                .map { (_, list) -> list.firstOrNull { it.hasFreeTrial } ?: list.first() }
                .sortedBy { it.type.ordinal }
        }

        override fun launchBillingFlow(
            activity: Activity,
            plan: SubscriptionPlan,
        ) {
            if (FORCE_ADS_OFF) {
                _isAdRemoved.value = true
                return
            }

            val details = productDetails ?: return

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(plan.offerToken)
                        .build(),
                )

            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }

        override fun onPurchasesUpdated(
            billingResult: BillingResult,
            purchases: MutableList<Purchase>?,
        ) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                acknowledgePurchases(purchases)
                // 결제 플로우 결과는 일부 집합이므로, 전체 활성 구독을 다시 조회해 상태를 권위 있게 재계산한다.
                queryPurchases()
            }
        }

        /**
         * 활성 구독 보유 여부로 광고 제거 상태를 true/false 양방향 설정한다(만료/취소 시 해제 포함).
         * queryPurchases 의 OK 결과만 호출되므로(오프라인/오류 시 미호출) 일시적 네트워크 오류로
         * 잘못 해제되지 않는다. 빈 목록(OK)은 Play 기준 "활성 구독 없음"의 권위 있는 신호다.
         */
        private fun updateAdRemovedState(purchases: List<Purchase>) {
            _isAdRemoved.value =
                purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.contains(SUBSCRIPTION_PRODUCT_ID)
                }
        }

        /** 광고 제거 구독 상품의 미승인(PURCHASED) 구매만 승인한다(타 상품 자동 승인 방지). */
        private fun acknowledgePurchases(purchases: List<Purchase>) {
            scope.launch {
                purchases
                    .filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            !it.isAcknowledged &&
                            it.products.contains(SUBSCRIPTION_PRODUCT_ID)
                    }
                    .forEach { purchase ->
                        val params =
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        billingClient.acknowledgePurchase(params) { _ -> }
                    }
            }
        }

        /** ISO 8601 결제 주기가 1년 이상이면 연간으로 본다(P1Y, P52W, P12M 등). */
        private fun isYearlyPeriod(period: String): Boolean {
            if (period.contains('Y')) return true
            val weeks = Regex("(\\d+)W").find(period)?.groupValues?.get(1)?.toIntOrNull()
            if (weeks != null && weeks >= 52) return true
            val months = Regex("(\\d+)M").find(period)?.groupValues?.get(1)?.toIntOrNull()
            return months != null && months >= 12
        }
    }
