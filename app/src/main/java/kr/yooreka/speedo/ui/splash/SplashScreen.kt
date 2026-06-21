package kr.yooreka.speedo.ui.splash

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.window.SplashScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateSubText

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(false) }
    // 위치(FINE/COARSE)는 핵심 권한: 거부 시 안내 후 앱 종료(F-06/§4.6).
    // 알림(POST_NOTIFICATIONS)은 게이트하지 않는다(옵션 A — 알림 없이도 핵심 동작 가능).
    var locationDenied by remember { mutableStateOf(false) }

    val permissionsToRequest =
        mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val locationGranted =
                results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            // 위치만 게이트: 알림 거부는 진행을 막지 않는다.
            if (locationGranted) permissionsGranted = true else locationDenied = true
        }

    LaunchedEffect(Unit) {
        val locationAlreadyGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (locationAlreadyGranted) {
            // 위치가 이미 허용된 경우 진행(알림은 재요청하지 않는다).
            permissionsGranted = true
        } else {
            launcher.launch(permissionsToRequest.toTypedArray())
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            delay(2000) // 2 seconds splash after permission decision
            onTimeout()
        }
    }

    // 위치 거부: 안내를 잠시 보여준 뒤 앱을 종료한다.
    LaunchedEffect(locationDenied) {
        if (locationDenied) {
            delay(2800)
            (context as? Activity)?.finish()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundBlack),
        contentAlignment = Alignment.Center,
    ) {
        // Background Glow
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .blur(192.dp)
                    .background(NeonGreen.copy(alpha = 0.1f), CircleShape),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Logo Container
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "EVERY BARI",
                color = NeonGreen,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
            )

            // Subtitle
            Text(
                text = "TELEMETRY SYSTEM",
                color = SlateSubText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )

            // 위치 권한 거부 안내(거부 시에만 표시 후 종료).
            if (locationDenied) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "위치 권한이 있어야 앱을 사용할 수 있습니다.\n권한을 허용한 뒤 다시 실행해 주세요.",
                    color = Color(0xFFFB2C36),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
            }
        }

        // Bottom Dot
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .size(6.dp)
                    .background(NeonGreen.copy(alpha = 0.7f), CircleShape),
        )
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen({})
}
