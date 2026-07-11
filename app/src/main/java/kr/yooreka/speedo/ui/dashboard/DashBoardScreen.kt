package kr.yooreka.speedo.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import kr.yooreka.speedo.ui.dashboard.components.AutoStopDialog
import kr.yooreka.speedo.ui.dashboard.components.LeanAngleLandscapeCard
import kr.yooreka.speedo.ui.dashboard.components.RecordingStartDialog
import kr.yooreka.speedo.ui.dashboard.components.RideStatsLandscapeCard
import kr.yooreka.speedo.ui.dashboard.components.SpeedometerCard
import kr.yooreka.speedo.ui.dashboard.components.SpeedometerOnlyCard
import kr.yooreka.speedo.ui.dashboard.components.TPMSCard
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.GreenSuccess
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SpeedoTheme

@Composable
fun DashBoardScreen(
    state: DashBoardState,
    uiEvent: SharedFlow<DashBoardUiEvent>,
    modifier: Modifier = Modifier,
    onRecordToggle: () -> Unit = {},
    onConfirmRecording: () -> Unit = {},
    onShowInterstitial: () -> Unit = {},
    onAutoStopContinue: () -> Unit = {},
    onAutoStopConfirm: () -> Unit = {},
    onMarkIssue: () -> Unit = {},
) {
    var showStartDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    // 콜백(비 Composable 람다)에서 리소스 직접 조회를 피하기 위해 미리 읽어 둔다.
    val issueMarkedMessage = stringResource(R.string.diagnostic_issue_marked)

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

    Box(modifier = modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(BackgroundBlack)
                        .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 가로모드 좌측 (6): 스피드미터 전용 카드
                Box(
                    modifier =
                        Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    SpeedometerOnlyCard(
                        speedKmh = state.speed,
                        speedUnit = state.speedUnit,
                        isRecording = state.isRecording,
                        brakeEvent = state.brakeEvent,
                        onMarkIssue = {
                            onMarkIssue()
                            Toast.makeText(context, issueMarkedMessage, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 가로모드 우측 (4): 뱅킹각(게이지 포함) 및 주행 정보 통계 카드
                Column(
                    modifier =
                        Modifier
                            .weight(0.8f)
                            .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LeanAngleLandscapeCard(
                        leanAngle = state.roll,
                        maxLeftRoll = state.maxLeftRoll,
                        maxRightRoll = state.maxRightRoll,
                        isRecording = state.isRecording,
                        modifier = Modifier.weight(1.2f),
                    )
                    RideStatsLandscapeCard(
                        duration = state.rideDuration,
                        distance = state.rideDistance,
                        speedUnit = state.speedUnit,
                        isRecording = state.isRecording,
                        isRecordingActive = state.isRecording,
                        onRecordToggle = onRecordToggle,
                        modifier = Modifier.weight(0.8f),
                    )
                }
            }
        } else {
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
                    isRecording = state.isRecording,
                    maxLeftRoll = state.maxLeftRoll,
                    maxRightRoll = state.maxRightRoll,
                    brakeEvent = state.brakeEvent,
                    onMarkIssue = {
                        onMarkIssue()
                        Toast.makeText(context, issueMarkedMessage, Toast.LENGTH_SHORT).show()
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                )
                if (state.showTpmsData) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TPMSCard(
                        rearPressure = state.rearPressure,
                        frontPressure = state.frontPressure,
                        rearTemp = state.rearTemp,
                        frontTemp = state.frontTemp,
                        rearBat = state.rearBat,
                        frontBat = state.frontBat,
                        pressureUnit = state.pressureUnit,
                        rearColor = state.rearPressureColor,
                        frontColor = state.frontPressureColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
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

        // 주행 종료 예상 감지(F-18): 기록 중 + 제안 상태일 때만 확인 다이얼로그 표시.
        // (기록 중이 아닐 때는 잔존 제안 상태로 다이얼로그가 뜨지 않도록 isRecording 으로 게이트)
        if (state.isRecording && state.autoStopSuggested) {
            AutoStopDialog(
                onContinue = onAutoStopContinue,
                onStop = onAutoStopConfirm,
            )
        }
    }
}

@Composable
fun DashBoardHeader(
    isRecording: Boolean,
    onRecordToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
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
                letterSpacing = (-0.53).sp,
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                color = Color(0xFF62748E),
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
                contentDescription = if (isRecording) stringResource(R.string.cd_stop) else stringResource(R.string.cd_start),
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRecording) stringResource(R.string.record_button_stop) else stringResource(R.string.record_button_start),
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
                    maxLeftRoll = "0°",
                    maxRightRoll = "0°",
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
                    maxLeftRoll = "0°",
                    maxRightRoll = "0°",
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
                maxLeftRoll = "24°",
                maxRightRoll = "28°",
            ),
            uiEvent = kotlinx.coroutines.flow.MutableSharedFlow(),
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - Recording No TPMS")
@Composable
fun DashBoardScreenRecordingNoTpmsPreview() {
    SpeedoTheme {
        DashBoardScreen(
            state =
                DashBoardState(
                    speed = "105",
                    roll = "8°",
                    isRecording = true,
                    showTpmsData = false,
                    maxLeftRoll = "28°",
                    maxRightRoll = "32°",
                ),
            uiEvent = kotlinx.coroutines.flow.MutableSharedFlow(),
        )
    }
}
