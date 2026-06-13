package kr.yooreka.speedo.ui.splash

import android.Manifest
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
            // We proceed regardless of whether all were granted,
            // but we can check if at least coarse location is granted
            permissionsGranted = true
        }

    LaunchedEffect(Unit) {
        val allGranted =
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

        if (allGranted) {
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
