package kr.yooreka.speedo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kr.yooreka.speedo.BuildConfig

// 구글 공식 테스트 배너 광고 단위 ID. debug 빌드에서 항상 채워지는 테스트 광고를 노출하며 클릭해도 정책 위반이 아니다.
private const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

// 프로덕션 배너 광고 단위 ID(release).
private const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6147358897182409/2455918735"

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID,
) {
    // 광고 비활성화(BuildConfig.ADS_ENABLED=false) 시 배너를 그리지 않는다.
    if (!BuildConfig.ADS_ENABLED) return

    val context = LocalContext.current

    // 실제 광고 로드 성공 여부. 로드 전/실패(NO_FILL 등) 시에는 영역 자체를 노출하지 않는다.
    var isLoaded by remember { mutableStateOf(false) }

    val adView =
        remember {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener =
                    object : AdListener() {
                        override fun onAdLoaded() {
                            isLoaded = true
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isLoaded = false
                        }
                    }
            }
        }

    DisposableEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
        onDispose { adView.destroy() }
    }

    // AdView 는 항상 컴포즈해 로드/자동 갱신을 유지하되, 로드 전에는 높이 0 으로 접어 빈 광고 영역이
    // 보이지 않게 한다. 로드된 경우에만 배경/테두리와 배너 높이(패딩 없음)를 노출한다.
    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (isLoaded) {
                        Modifier
                            .background(Color(0xFF0F172A))
                            .border(0.6.dp, Color(0xFF1E293B))
                    } else {
                        Modifier.height(0.dp)
                    },
                ),
        factory = { adView },
    )
}
