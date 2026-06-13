package kr.yooreka.speedo.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private var interstitialAd: InterstitialAd? = null
        private var isAdLoading = false

        // Default Interstitial Test Ad Unit ID
        // If you have a specific interstitial ad unit ID, replace this here.
        private val adUnitId = "ca-app-pub-3940256099942544/1033173712"

        fun loadInterstitialAd() {
            if (interstitialAd != null || isAdLoading) {
                return
            }

            isAdLoading = true
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                context,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        interstitialAd = null
                        isAdLoading = false
                    }

                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isAdLoading = false
                    }
                },
            )
        }

        fun showInterstitial(
            activity: Activity,
            onAdDismissed: () -> Unit,
        ) {
            if (interstitialAd != null) {
                interstitialAd?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            onAdDismissed()
                            loadInterstitialAd() // Pre-load next ad
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            onAdDismissed()
                        }
                    }
                interstitialAd?.show(activity)
            } else {
                // Ad wasn't ready, proceed immediately
                onAdDismissed()
                loadInterstitialAd() // Try to load one for next time
            }
        }
    }
