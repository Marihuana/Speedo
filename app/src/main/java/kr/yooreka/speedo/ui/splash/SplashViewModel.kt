package kr.yooreka.speedo.ui.splash

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.billing.BillingRepository
import kr.yooreka.speedo.data.local.preferences.UserPreferencesRepository
import kr.yooreka.speedo.utils.AdManager
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val billingRepository: BillingRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            userPreferencesRepository.incrementLaunchCount()
        }
    }

    fun onSplashFinished(activity: Activity, adManager: AdManager, onComplete: () -> Unit) {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val isAdRemoved = billingRepository.isAdRemoved.first()

            if (prefs.launchCount % 3 == 0 && !isAdRemoved) {
                adManager.showInterstitial(activity, onComplete)
            } else {
                onComplete()
            }
        }
    }
}
