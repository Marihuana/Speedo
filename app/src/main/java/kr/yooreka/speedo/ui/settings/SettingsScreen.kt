package kr.yooreka.speedo.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.yooreka.speedo.R
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.ui.components.BannerAd
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateDark
import kr.yooreka.speedo.ui.theme.SlateSubText
import kr.yooreka.speedo.ui.theme.SlateText
import kr.yooreka.speedo.ui.theme.SpeedoTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 콜백(비 Composable 람다)에서 사용하기 위해 문자열 리소스를 미리 읽어 둔다.
    val calibrationResetMessage = stringResource(R.string.calibration_reset)

    SettingsContent(
        speedUnit = state.speedUnit,
        onSpeedUnitChange = { viewModel.updateSpeedUnit(it) },
        isCalibrating = state.isCalibrating,
        onCalibrateClick = { viewModel.calibrate() },
        onResetCalibration = {
            viewModel.resetCalibration()
            Toast.makeText(context, calibrationResetMessage, Toast.LENGTH_SHORT).show()
        },
        isAdRemoved = state.isAdRemoved,
        onPurchaseRemoveAds = {
            (context as? android.app.Activity)?.let { activity ->
                viewModel.purchaseRemoveAds(activity)
            }
        },
        selectedLeanMode = state.leanMeasurementMode,
        onLeanModeChange = { viewModel.updateLeanMeasurementMode(it) },
        onExportDiagnostics = { shareDiagnosticCsv(context, viewModel.diagnosticCsvFiles()) },
    )
}

/** 진단 CSV(F-03)를 메일로 전송한다. 파일이 없으면 안내만 표시. */
private fun shareDiagnosticCsv(
    context: Context,
    files: List<java.io.File>,
) {
    if (files.isEmpty()) {
        Toast.makeText(context, "전송할 진단 데이터가 없습니다. 먼저 주행을 기록하세요.", Toast.LENGTH_SHORT).show()
        return
    }
    val authority = "${context.packageName}.fileprovider"
    val uris =
        ArrayList(
            files.map { androidx.core.content.FileProvider.getUriForFile(context, authority, it) },
        )
    val intent =
        android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(DIAGNOSTIC_EMAIL))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Speedo lean diagnostics")
            putExtra(android.content.Intent.EXTRA_TEXT, "lean 측정 진단 CSV(${files.size}개)를 첨부합니다.")
            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(android.content.Intent.createChooser(intent, "Export diagnostics"))
}

/** 진단 CSV Export 수신자(개발자). */
private const val DIAGNOSTIC_EMAIL = "bracket0723@gmail.com"

@Composable
fun SettingsContent(
    speedUnit: String,
    onSpeedUnitChange: (String) -> Unit,
    isCalibrating: Boolean = false,
    onCalibrateClick: () -> Unit = {},
    onResetCalibration: () -> Unit = {},
    isAdRemoved: Boolean = false,
    onPurchaseRemoveAds: () -> Unit = {},
    selectedLeanMode: LeanMode = LeanMode.DEFAULT,
    onLeanModeChange: (LeanMode) -> Unit = {},
    onExportDiagnostics: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundBlack),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            SettingsHeader()

            // Display Section
            SettingsSectionHeader(title = "Display", iconRes = kr.yooreka.speedo.R.drawable.ic_monitor)
            DisplayCard(
                speedUnit = speedUnit,
                onSpeedUnitChange = onSpeedUnitChange,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Sensors & BLE Section
            SettingsSectionHeader(title = "Sensors & BLE", iconRes = kr.yooreka.speedo.R.drawable.ic_records)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CalibrationCard(
                    isCalibrating = isCalibrating,
                    onCalibrateClick = onCalibrateClick,
                    onResetClick = onResetCalibration,
                )
                LeanMeasurementCard(
                    selectedMode = selectedLeanMode,
                    onModeSelected = onLeanModeChange,
                    onExportDiagnostics = onExportDiagnostics,
                )
            }

            if (!isAdRemoved) {
                Spacer(modifier = Modifier.height(32.dp))

                // Account & Upgrades Section
                SettingsSectionHeader(title = "Account & Upgrades", iconRes = kr.yooreka.speedo.R.drawable.ic_premium)
                PremiumCard(
                    onPurchaseClick = onPurchaseRemoveAds,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Version Info
            Text(
                text = "Every Bari Telemetry v1.2.0",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                color = Color(0xFF45556C),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.11.sp,
            )
        }

        if (!isAdRemoved) {
            BannerAd(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .border(0.6.dp, Color(0xFF1E293B))
                        .padding(top = 12.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
fun PremiumCard(onPurchaseClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                // Linear gradient is complex to compose directly without shape, using a simpler approach or Brush
                .background(
                    Color.Transparent,
                ),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(0.6.dp, Color(0xFFFE9A00).copy(alpha = 0.2f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color(0xFFFE9A00).copy(alpha = 0.1f),
                                        Color.Transparent,
                                    ),
                            ),
                        ),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Premium Version",
                        color = Color(0xFFFFB900),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.71).sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remove all ads permanently",
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color(0xFFFFB900), RoundedCornerShape(16.dp))
                                .clickable { onPurchaseClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Purchase - \$4.00",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.25.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DisplayCard(
    speedUnit: String,
    onSpeedUnitChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // 속도 단위 선택. TPMS(압력) 관련 항목은 이번 버전 비활성화로 제외.
            UnitSelector(
                label = "Speed Unit",
                options = listOf("KM/H", "MPH"),
                selectedOption = speedUnit,
                onOptionSelected = onSpeedUnitChange,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalibrationCard(
    isCalibrating: Boolean = false,
    onCalibrateClick: () -> Unit,
    onResetClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Lean Angle Calibration",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.71).sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reset sensor zero point",
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color(0xFF1D293D), RoundedCornerShape(16.dp))
                        .combinedClickable(
                            enabled = !isCalibrating,
                            onClick = onCalibrateClick,
                            onLongClick = onResetClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text =
                        if (isCalibrating) {
                            stringResource(R.string.calibrating).uppercase()
                        } else {
                            "CALIBRATE ZERO POINT"
                        },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.25.sp,
                )
            }
        }
    }
}

/** 측정 방식(F-03) 표시 라벨. 실측 비교용 5종. */
private fun leanModeLabel(mode: LeanMode): String =
    when (mode) {
        LeanMode.GRAVITY_TILT -> "GRAVITY (기본)"
        LeanMode.ACCEL_TILT -> "ACCELEROMETER"
        LeanMode.ROTATION_VECTOR -> "ROTATION VECTOR"
        LeanMode.GAME_ROTATION_VECTOR -> "GAME ROTATION VECTOR"
        LeanMode.COMPLEMENTARY -> "COMPLEMENTARY (자이로 융합)"
    }

/**
 * lean 측정 전략 선택 카드(F-03). 활성 전략을 즉시 교체하며, 실주행 비교 후 가장 정확한 방식을
 * 채택하기 위한 비교 도구다.
 */
@Composable
fun LeanMeasurementCard(
    selectedMode: LeanMode,
    onModeSelected: (LeanMode) -> Unit,
    onExportDiagnostics: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Lean Measurement",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.71).sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Compare strategies, then keep the most accurate",
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

            LeanMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(18.dp)
                                .background(
                                    if (isSelected) NeonGreen else Color.Transparent,
                                    CircleShape,
                                )
                                .border(
                                    1.5.dp,
                                    if (isSelected) NeonGreen else Color(0xFF45556C),
                                    CircleShape,
                                ),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = leanModeLabel(mode),
                        color = if (isSelected) Color.White else SlateText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF314158), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 진단 CSV는 주행 기록 중 자동 저장되며, 아래 버튼으로 개발자에게 메일 전송한다.
            Text(
                text = "Diagnostic logs are saved automatically while recording a ride.",
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.12.sp,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onExportDiagnostics,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D293D)),
                border = BorderStroke(0.6.dp, Color(0xFF314158)),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = "EXPORT MEASUREMENTS",
                    color = Color(0xFFCAD5E2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

@Composable
fun SettingsHeader() {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 32.dp)) {
        Text(
            text = stringResource(R.string.settings_title),
            color = NeonGreen,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = stringResource(R.string.settings_subtitle),
            color = SlateSubText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    iconRes: Int,
) {
    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color(0xFFCAD5E2),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color(0xFFCAD5E2),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
fun UnitSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Row(
            modifier =
                Modifier
                    .background(Color.Black, RoundedCornerShape(16.dp))
                    .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Box(
                    modifier =
                        Modifier
                            .height(40.dp)
                            .background(
                                if (isSelected) NeonGreen else Color.Transparent,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onOptionSelected(option) }
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.Black else SlateText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.15).sp,
                    )
                }
            }
        }
    }
}

// Previews
@Preview(showBackground = true, name = "Display Card")
@Composable
fun DisplayCardPreview() {
    SpeedoTheme {
        Surface(color = BackgroundBlack, modifier = Modifier.padding(16.dp)) {
            DisplayCard(
                speedUnit = "KM/H",
                onSpeedUnitChange = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings - Full Screen")
@Composable
fun SettingsScreenFullPreview() {
    SpeedoTheme {
        SettingsContent(
            speedUnit = "KM/H",
            onSpeedUnitChange = {},
        )
    }
}
