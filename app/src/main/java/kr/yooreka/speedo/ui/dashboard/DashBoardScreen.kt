package kr.yooreka.speedo.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.dashboard.components.RecordingStartDialog
import kr.yooreka.speedo.ui.dashboard.components.SpeedometerCard
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.GreenSuccess
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateSubText
import kr.yooreka.speedo.ui.theme.SpeedoTheme

@Composable
fun DashBoardScreen(
    state: DashBoardState,
    uiEvent: SharedFlow<DashBoardUiEvent>,
    onRecordToggle: () -> Unit = {},
    onConfirmRecording: () -> Unit = {},
    onShowInterstitial: () -> Unit = {},
) {
    var showStartDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            uiEvent.collect { event ->
                when (event) {
                    is DashBoardUiEvent.ShowStartDialog -> showStartDialog = true
                    is DashBoardUiEvent.ShowInterstitialAd -> onShowInterstitial()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BackgroundBlack)
                    .padding(20.dp),
        ) {
            DashBoardHeader(
                isRecording = state.isRecording,
                onRecordToggle = onRecordToggle,
            )
            SpeedometerCard(
                speedKmh = state.speed,
                leanAngle = state.roll,
                speedUnit = state.speedUnit,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
            // TPMS 카드는 이번 버전 비활성화(F-07)로 노출하지 않는다(백엔드/모델은 보존).
        }

        if (showStartDialog) {
            RecordingStartDialog(
                onDismiss = { showStartDialog = false },
                onConfirm = {
                    showStartDialog = false
                    onConfirmRecording()
                },
            )
        }
    }
}

@Composable
fun DashBoardHeader(
    isRecording: Boolean,
    onRecordToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.dashboard_title),
                color = NeonGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                color = SlateSubText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
        }

        Button(
            onClick = onRecordToggle,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFFB2C36) else NeonGreen,
                    contentColor = if (isRecording) Color.White else Color.Black,
                ),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                painter =
                    painterResource(
                        id = if (isRecording) R.drawable.ic_stop else R.drawable.ic_play,
                    ),
                contentDescription = if (isRecording) "Stop" else "Start",
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRecording) "정지" else "기록",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Header - Normal")
@Composable
fun DashBoardHeaderNormalPreview() {
    SpeedoTheme {
        Box(modifier = Modifier.padding(16.dp).background(BackgroundBlack)) {
            DashBoardHeader(isRecording = false, onRecordToggle = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Header - Recording")
@Composable
fun DashBoardHeaderRecordingPreview() {
    SpeedoTheme {
        Box(modifier = Modifier.padding(16.dp).background(BackgroundBlack)) {
            DashBoardHeader(isRecording = true, onRecordToggle = {})
        }
    }
}

@Preview(showBackground = true, name = "Dashboard - Basic")
@Composable
fun DashBoardScreenBasicPreview() {
    SpeedoTheme {
        DashBoardScreen(
            state =
                DashBoardState(
                    speed = "80",
                    roll = "19°",
                    showTpmsData = false,
                ),
            uiEvent = kotlinx.coroutines.flow.MutableSharedFlow(),
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - With TPMS")
@Composable
fun DashBoardScreenWithTpmsPreview() {
    SpeedoTheme {
        DashBoardScreen(
            state =
                DashBoardState(
                    speed = "61",
                    roll = "19°",
                    showTpmsData = true,
                    frontPressure = "36.2",
                    rearPressure = "38.5",
                    frontTemp = "42°",
                    rearTemp = "45°",
                    frontBat = "2.8V",
                    rearBat = "2.7V",
                    frontPressureColor = GreenSuccess,
                    rearPressureColor = GreenSuccess,
                ),
            uiEvent = kotlinx.coroutines.flow.MutableSharedFlow(),
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - Recording")
@Composable
fun DashBoardScreenRecordingPreview() {
    SpeedoTheme {
        DashBoardScreen(
            state =
                DashBoardState(
                    speed = "101",
                    roll = "4°",
                    isRecording = true,
                    showTpmsData = true,
                    frontPressure = "36.2",
                    rearPressure = "38.5",
                    frontTemp = "42°",
                    rearTemp = "45°",
                    frontBat = "2.8V",
                    rearBat = "2.7V",
                    frontPressureColor = GreenSuccess,
                    rearPressureColor = GreenSuccess,
                ),
            uiEvent = kotlinx.coroutines.flow.MutableSharedFlow(),
        )
    }
}
