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
            private const val PREMIUM_PRODUCT_ID = "remove_ads_premium"
        }

        // @Singleton 수명과 함께 사는 구조적 스코프(즉석 CoroutineScope 생성 금지).
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private val _isAdRemoved = MutableStateFlow(FORCE_ADS_OFF)
        override val isAdRemoved: StateFlow<Boolean> = _isAdRemoved.asStateFlow()

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
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases)
                }
            }
        }

        private fun queryProductDetails() {
            val queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(PREMIUM_PRODUCT_ID)
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                        ),
                    )
                    .build()

            billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    productDetails = productDetailsList.first()
                }
            }
        }

        override fun launchBillingFlow(activity: Activity) {
            if (FORCE_ADS_OFF) {
                _isAdRemoved.value = true
                return
            }

            val details = productDetails ?: return

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
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
                processPurchases(purchases)
            }
        }

        private fun processPurchases(purchases: List<Purchase>) {
            scope.launch {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        if (purchase.products.contains(PREMIUM_PRODUCT_ID)) {
                            _isAdRemoved.value = true

                            if (!purchase.isAcknowledged) {
                                val acknowledgePurchaseParams =
                                    AcknowledgePurchaseParams.newBuilder()
                                        .setPurchaseToken(purchase.purchaseToken)
                                        .build()
                                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { _ -> }
                            }
                        }
                    }
                }
            }
        }
    }
