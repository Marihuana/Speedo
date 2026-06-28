package kr.yooreka.speedo.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kr.yooreka.speedo.BuildConfig

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-6147358897182409/2455918735",
) {
    // 알파 기간 등 광고 비활성화(BuildConfig.ADS_ENABLED=false) 시 배너를 그리지 않는다.
    if (!BuildConfig.ADS_ENABLED) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
        update = {
            it.loadAd(AdRequest.Builder().build())
        },
    )
}
