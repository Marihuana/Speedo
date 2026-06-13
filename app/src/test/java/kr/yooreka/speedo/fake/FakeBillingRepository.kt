package kr.yooreka.speedo.fake

import android.app.Activity
import kr.yooreka.speedo.data.billing.BillingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [BillingRepository] Fake. 결제/Play Billing 없이 광고 제거 상태를 제어한다.
 */
class FakeBillingRepository(
    initialAdRemoved: Boolean = false,
) : BillingRepository {

    private val _isAdRemoved = MutableStateFlow(initialAdRemoved)
    override val isAdRemoved: StateFlow<Boolean> = _isAdRemoved

    var launchCount = 0
        private set

    fun setAdRemoved(value: Boolean) {
        _isAdRemoved.value = value
    }

    override fun launchBillingFlow(activity: Activity) {
        launchCount++
    }
}
