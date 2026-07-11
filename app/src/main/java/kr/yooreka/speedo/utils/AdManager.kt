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
import kr.yooreka.speedo.BuildConfig
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

        // 광고 단위 ID: debug=구글 공식 테스트 전면광고 ID(항상 채워짐, 클릭해도 정책 위반 아님), release=프로덕션 ID.
        private val adUnitId =
            if (BuildConfig.DEBUG) {
                "ca-app-pub-3940256099942544/1033173712"
            } else {
                "ca-app-pub-6147358897182409/9540201834"
            }

        fun loadInterstitialAd() {
            // 광고 비활성화(알파 등) 시 로드하지 않는다.
            if (!BuildConfig.ADS_ENABLED) return
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
            // 광고 비활성화(알파 등) 시 광고를 띄우지 않고 즉시 진행한다.
            if (!BuildConfig.ADS_ENABLED) {
                onAdDismissed()
                return
            }
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
