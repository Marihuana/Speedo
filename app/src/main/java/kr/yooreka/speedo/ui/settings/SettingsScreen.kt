package kr.yooreka.speedo.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import kr.yooreka.speedo.domain.model.DonationProduct
import kr.yooreka.speedo.domain.model.LeanMode
import kr.yooreka.speedo.domain.model.OverlayMode
import kr.yooreka.speedo.domain.model.OverlaySettings
import kr.yooreka.speedo.domain.model.OverlaySize
import kr.yooreka.speedo.domain.model.SubscriptionPlan
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

    // 오버레이 권한 안내 다이얼로그 표시 여부(F-19, §4.4).
    var showOverlayRationale by remember { mutableStateOf(false) }
    // 권한 설정 화면에서 돌아온 뒤 허용되었으면 오버레이를 켠다.
    val overlayPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Settings.canDrawOverlays(context)) {
                viewModel.updateOverlayEnabled(true)
            }
        }

    if (showOverlayRationale) {
        OverlayPermissionDialog(
            onConfirm = {
                showOverlayRationale = false
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ),
                )
            },
            onDismiss = { showOverlayRationale = false },
        )
    }

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
        subscriptionPlans = state.subscriptionPlans,
        onPurchasePlan = { plan ->
            (context as? android.app.Activity)?.let { activity ->
                viewModel.purchasePlan(activity, plan)
            }
        },
        donationProduct = state.donationProduct,
        onDonateClick = {
            (context as? android.app.Activity)?.let { activity ->
                viewModel.donate(activity)
            }
        },
        selectedLeanMode = state.leanMeasurementMode,
        onLeanModeChange = { viewModel.updateLeanMeasurementMode(it) },
        onExportDiagnostics = { shareDiagnosticCsv(context, viewModel.diagnosticCsvFiles()) },
        autoStopThresholdMin = state.autoStopThresholdMin,
        onAutoStopThresholdChange = { viewModel.updateAutoStopThreshold(it) },
        overlaySettings = state.overlaySettings,
        onOverlayEnabledChange = { enabled ->
            // 켜는데 권한이 없으면 안내 다이얼로그 → 시스템 설정 이동(§4.4). 끄는 동작은 즉시 반영.
            if (enabled && !Settings.canDrawOverlays(context)) {
                showOverlayRationale = true
            } else {
                viewModel.updateOverlayEnabled(enabled)
            }
        },
        onOverlayModeChange = { viewModel.updateOverlayMode(it) },
        onOverlaySizeChange = { viewModel.updateOverlaySize(it) },
        onOverlayOpacityChange = { viewModel.updateOverlayOpacity(it) },
    )
}

/** 진단 CSV(F-03)를 메일로 전송한다. 파일이 없으면 안내만 표시. */
private fun shareDiagnosticCsv(
    context: Context,
    files: List<java.io.File>,
) {
    if (files.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.diagnostic_no_data), Toast.LENGTH_SHORT).show()
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
            putExtra(android.content.Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostic_email_subject))
            putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.diagnostic_email_body, files.size))
            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, uris)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.diagnostic_chooser_title)))
}

/** 진단 CSV Export 수신자(개발자). */
private const val DIAGNOSTIC_EMAIL = "bracket0723.dev@gmail.com"

@Composable
fun SettingsContent(
    speedUnit: String,
    onSpeedUnitChange: (String) -> Unit,
    isCalibrating: Boolean = false,
    onCalibrateClick: () -> Unit = {},
    onResetCalibration: () -> Unit = {},
    isAdRemoved: Boolean = false,
    subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    onPurchasePlan: (SubscriptionPlan) -> Unit = {},
    donationProduct: DonationProduct? = null,
    onDonateClick: () -> Unit = {},
    selectedLeanMode: LeanMode = LeanMode.DEFAULT,
    onLeanModeChange: (LeanMode) -> Unit = {},
    onExportDiagnostics: () -> Unit = {},
    autoStopThresholdMin: Int = 5,
    onAutoStopThresholdChange: (Int) -> Unit = {},
    overlaySettings: OverlaySettings = OverlaySettings(),
    onOverlayEnabledChange: (Boolean) -> Unit = {},
    onOverlayModeChange: (OverlayMode) -> Unit = {},
    onOverlaySizeChange: (OverlaySize) -> Unit = {},
    onOverlayOpacityChange: (Int) -> Unit = {},
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
            SettingsSectionHeader(title = stringResource(R.string.section_display), iconRes = kr.yooreka.speedo.R.drawable.ic_monitor)
            DisplayCard(
                speedUnit = speedUnit,
                onSpeedUnitChange = onSpeedUnitChange,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Sensors & BLE Section
            SettingsSectionHeader(title = stringResource(R.string.section_sensors), iconRes = kr.yooreka.speedo.R.drawable.ic_records)
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
                AutoStopCard(
                    thresholdMin = autoStopThresholdMin,
                    onThresholdChange = onAutoStopThresholdChange,
                )
                OverlayCard(
                    settings = overlaySettings,
                    onEnabledChange = onOverlayEnabledChange,
                    onModeChange = onOverlayModeChange,
                    onSizeChange = onOverlaySizeChange,
                    onOpacityChange = onOverlayOpacityChange,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Account & Upgrades Section
            SettingsSectionHeader(title = stringResource(R.string.section_account), iconRes = kr.yooreka.speedo.R.drawable.ic_premium)
            if (isAdRemoved) {
                // 구독 중: 활성 상태 안내 카드.
                PremiumActiveCard()
            } else {
                PremiumCard(
                    plans = subscriptionPlans,
                    onPurchasePlan = onPurchasePlan,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Version Info
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "App Icon",
                    modifier =
                        Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = 30.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = NeonGreen.copy(alpha = 0.15f),
                                spotColor = NeonGreen.copy(alpha = 0.15f),
                            ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.app_version).uppercase(),
                    color = Color(0xFF45556C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.11.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // 개발자 오토바이 사주기 이스터에그(F-24): 버전 정보 칩 아래 최하단에 배치.
            // 상품이 조회된 경우에만 노출한다.
            if (donationProduct != null) {
                DonationCard(onDonateClick = onDonateClick)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (!isAdRemoved) {
            // 광고가 실제 로드된 경우에만 영역이 노출된다(배경/테두리는 BannerAd 내부에서 처리).
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 개발자 후원(오토바이 사주기) 2중 컨펌 플로우 단계(F-24). */
private enum class DonationConfirmStage { NONE, FIRST, SECOND }

/**
 * 개발자 후원(오토바이 사주기) 카드(F-24). 일회성 인앱 결제 상품이 조회된 경우에만 노출.
 * 버튼 클릭 시 즉시 결제하지 않고 1차·2차 컨펌 다이얼로그를 거친 뒤에만 [onDonateClick](실제 IAP)을 호출한다.
 */
@Composable
fun DonationCard(
    onDonateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 다이얼로그 표시 단계는 카드 로컬 UI 상태(드롭다운 expanded 등과 동일한 성격)로 관리한다.
    var confirmStage by remember { mutableStateOf(DonationConfirmStage.NONE) }
    val amount = stringResource(R.string.donation_amount)

    when (confirmStage) {
        DonationConfirmStage.NONE -> Unit
        DonationConfirmStage.FIRST ->
            DonationConfirmDialog(
                message = stringResource(R.string.donation_confirm_first_message, amount),
                confirmText = stringResource(R.string.donation_confirm_first_confirm),
                dismissText = stringResource(R.string.donation_confirm_first_dismiss),
                onConfirm = { confirmStage = DonationConfirmStage.SECOND },
                onDismiss = { confirmStage = DonationConfirmStage.NONE },
            )
        DonationConfirmStage.SECOND ->
            DonationConfirmDialog(
                message = stringResource(R.string.donation_confirm_second_message),
                confirmText = stringResource(R.string.donation_confirm_second_confirm),
                dismissText = stringResource(R.string.donation_confirm_second_dismiss),
                onConfirm = {
                    confirmStage = DonationConfirmStage.NONE
                    // 2차 컨펌까지 통과한 경우에만 실제 결제(donate→launchDonation) 경로를 호출한다.
                    onDonateClick()
                },
                onDismiss = { confirmStage = DonationConfirmStage.NONE },
            )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.6.dp, NeonGreen.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors =
                                listOf(
                                    NeonGreen.copy(alpha = 0.1f),
                                    Color.Transparent,
                                ),
                        ),
                    ),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🏍️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.donation_title),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.71).sp,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.donation_desc),
                            color = SlateSubText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    // 즉시 결제 대신 2중 컨펌 플로우의 1단계를 연다.
                    onClick = { confirmStage = DonationConfirmStage.FIRST },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = Color.Black,
                        ),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.donation_button_easter_egg, amount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp,
                    )
                }
            }
        }
    }
}

/** 구독(광고 제거) 활성 상태 안내 카드. `isAdRemoved=true` 일 때 노출. */
@Composable
fun PremiumActiveCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFFFB900),
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.premium_active_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.71).sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.premium_active_desc),
                        color = Color(0xFFFFB900),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumCard(
    plans: List<SubscriptionPlan>,
    onPurchasePlan: (SubscriptionPlan) -> Unit,
) {
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
                        text = stringResource(R.string.premium_version),
                        color = Color(0xFFFFB900),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.71).sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.premium_desc),
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (plans.isEmpty()) {
                        // 상품 조회 전/실패 시: 로딩 안내만 노출(결제 버튼 미노출).
                        Text(
                            text = stringResource(R.string.premium_loading),
                            color = SlateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            plans.forEach { plan ->
                                SubscriptionPlanButton(
                                    plan = plan,
                                    onClick = { onPurchasePlan(plan) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionPlanButton(
    plan: SubscriptionPlan,
    onClick: () -> Unit,
) {
    val periodLabel =
        when (plan.type) {
            SubscriptionPlan.PlanType.MONTHLY -> stringResource(R.string.premium_plan_monthly)
            SubscriptionPlan.PlanType.YEARLY -> stringResource(R.string.premium_plan_yearly)
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFFFB900), RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = stringResource(R.string.premium_plan_format, periodLabel, plan.formattedPrice),
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
            )
            if (plan.hasFreeTrial) {
                Text(
                    text = stringResource(R.string.premium_free_trial),
                    color = Color.Black.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
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
                label = stringResource(R.string.speed_unit_label),
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
                text = stringResource(R.string.lean_calibration_label),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.71).sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.lean_calibration_desc),
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
                        .height(63.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = NeonGreen.copy(alpha = 0.15f),
                            spotColor = NeonGreen.copy(alpha = 0.15f),
                        )
                        .background(Color.Transparent, RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.3.dp, NeonGreen), RoundedCornerShape(16.dp))
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
                            stringResource(R.string.calibrate_button)
                        },
                    color = NeonGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = 1.25.sp,
                )
            }
        }
    }
}

/**
 * 측정 방식(F-03) 표시 라벨. 실측 비교용 5종.
 * 기술 용어(GRAVITY 등)는 영어 그대로 두고, 보조 표기(기본/자이로 융합)만 OS 언어로 치환한다.
 */
@Composable
private fun leanModeLabel(mode: LeanMode): String =
    when (mode) {
        LeanMode.GRAVITY_TILT -> stringResource(R.string.lean_mode_gravity)
        LeanMode.ACCEL_TILT -> stringResource(R.string.lean_mode_accelerometer)
        LeanMode.ROTATION_VECTOR -> stringResource(R.string.lean_mode_rotation_vector)
        LeanMode.GAME_ROTATION_VECTOR -> stringResource(R.string.lean_mode_game_rotation_vector)
        LeanMode.COMPLEMENTARY -> stringResource(R.string.lean_mode_complementary)
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
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.lean_measurement_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.71).sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.lean_measurement_desc),
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(20.dp))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val menuWidth = maxWidth
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(65.dp)
                            .background(Color(0xFF151F30), RoundedCornerShape(14.dp))
                            .border(BorderStroke(0.7.dp, Color(0xFF314158)), RoundedCornerShape(14.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = leanModeLabel(selectedMode),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown Arrow",
                        tint = Color(0xFFCAD5E2),
                        modifier = Modifier.size(24.dp),
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier =
                        Modifier
                            .width(menuWidth)
                            .background(Color(0xFF1D293D))
                            .border(BorderStroke(0.6.dp, Color(0xFF314158)), RoundedCornerShape(12.dp)),
                ) {
                    LeanMode.entries.forEachIndexed { index, mode ->
                        val isSelected = mode == selectedMode
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = leanModeLabel(mode),
                                    color = if (isSelected) NeonGreen else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                )
                            },
                            onClick = {
                                onModeSelected(mode)
                                expanded = false
                            },
                            trailingIcon =
                                if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = NeonGreen,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                } else {
                                    null
                                },
                            modifier =
                                Modifier
                                    .height(53.dp)
                                    .background(
                                        if (isSelected) Color(0xFF25354F) else Color.Transparent,
                                    ),
                        )
                        if (index < LeanMode.entries.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFF314158).copy(alpha = 0.6f),
                                thickness = 0.7.dp,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF314158), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 진단 CSV는 주행 기록 중 자동 저장되며, 아래 버튼으로 개발자에게 메일 전송한다.
            Text(
                text = stringResource(R.string.diagnostic_logs_desc),
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
                    text = stringResource(R.string.export_measurements),
                    color = Color(0xFFCAD5E2),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

/**
 * 주행 종료 예상 감지 설정(F-18a). ON/OFF 스위치 + (ON일 때) 3/5/10분 선택.
 * thresholdMin 0=OFF, >0=ON(분).
 */
@Composable
fun AutoStopCard(
    thresholdMin: Int,
    onThresholdChange: (Int) -> Unit,
) {
    val enabled = thresholdMin > 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.auto_stop_detection_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.71).sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.auto_stop_detection_desc),
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                        lineHeight = 15.sp,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { on -> onThresholdChange(if (on) 5 else 0) },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.Black,
                            uncheckedTrackColor = Color(0xFF314158),
                            uncheckedBorderColor = Color.Transparent,
                        ),
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF314158), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                UnitSelector(
                    label = stringResource(R.string.auto_stop_detection_time),
                    options = listOf("3", "5", "10"),
                    selectedOption = thresholdMin.toString(),
                    onOptionSelected = { onThresholdChange(it.toInt()) },
                )
            }
        }
    }
}

/**
 * 플로팅 오버레이 위젯 설정 카드(F-19a/F-19b).
 * 사용 토글 + (활성 시) 모드/크기/투명도 조절을 제공한다. 권한 요청은 상위(SettingsScreen)에서 처리.
 */
@Composable
fun OverlayCard(
    settings: OverlaySettings,
    onEnabledChange: (Boolean) -> Unit,
    onModeChange: (OverlayMode) -> Unit,
    onSizeChange: (OverlaySize) -> Unit,
    onOpacityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.overlay_title),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.71).sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.overlay_desc),
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                        lineHeight = 15.sp,
                    )
                }
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = onEnabledChange,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.Black,
                            uncheckedTrackColor = Color(0xFF314158),
                            uncheckedBorderColor = Color.Transparent,
                        ),
                )
            }

            if (settings.enabled) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF314158), thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                OverlayOptionSelector(
                    label = stringResource(R.string.overlay_mode_label),
                    options =
                        listOf(
                            OverlayMode.SPEEDOMETER to stringResource(R.string.overlay_mode_speed),
                            OverlayMode.LEAN to stringResource(R.string.overlay_mode_lean),
                            OverlayMode.COMBINED to stringResource(R.string.overlay_mode_combined),
                        ),
                    selected = settings.mode,
                    onSelect = onModeChange,
                )

                Spacer(modifier = Modifier.height(20.dp))

                OverlayOptionSelector(
                    label = stringResource(R.string.overlay_size_label),
                    options =
                        listOf(
                            OverlaySize.SMALL to stringResource(R.string.overlay_size_small),
                            OverlaySize.MEDIUM to stringResource(R.string.overlay_size_medium),
                            OverlaySize.LARGE to stringResource(R.string.overlay_size_large),
                        ),
                    selected = settings.size,
                    onSelect = onSizeChange,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.overlay_opacity_label).uppercase(),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                    )
                    Text(
                        text = "${settings.opacity}%",
                        color = NeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Slider(
                    value = settings.opacity.toFloat(),
                    onValueChange = { onOpacityChange(it.toInt()) },
                    valueRange = 20f..100f,
                    colors =
                        SliderDefaults.colors(
                            thumbColor = NeonGreen,
                            activeTrackColor = NeonGreen,
                            inactiveTrackColor = Color(0xFF314158),
                        ),
                )
            }
        }
    }
}

/** 오버레이 설정용 세그먼트 선택기. [UnitSelector]와 동일한 비주얼을 enum 값 기준으로 제공한다. */
@Composable
private fun <T> OverlayOptionSelector(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
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
            options.forEach { (value, text) ->
                val isSelected = value == selected
                Box(
                    modifier =
                        Modifier
                            .height(40.dp)
                            .background(
                                if (isSelected) NeonGreen else Color.Transparent,
                                RoundedCornerShape(14.dp),
                            )
                            .clickable { onSelect(value) }
                            .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = text,
                        color = if (isSelected) Color.Black else SlateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.15).sp,
                    )
                }
            }
        }
    }
}

/**
 * 개발자 후원(오토바이 사주기) 컨펌 다이얼로그(F-24).
 * 1차/2차 공용으로 쓰이며, 확인/취소 문구와 콜백만 주입받는다.
 */
@Composable
private fun DonationConfirmDialog(
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateDark,
        title = {
            Text(
                text = stringResource(R.string.donation_title),
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Text(
                text = message,
                color = SlateText,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText, color = NeonGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, color = SlateText)
            }
        },
    )
}

/** 오버레이 권한 안내 다이얼로그(§4.4). 확인 시 시스템 오버레이 권한 설정으로 이동한다. */
@Composable
private fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateDark,
        title = {
            Text(
                text = stringResource(R.string.overlay_permission_title),
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.overlay_permission_desc),
                color = SlateText,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.overlay_permission_confirm), color = NeonGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.overlay_permission_cancel), color = SlateText)
            }
        },
    )
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

@Preview(showBackground = true, name = "Premium Active")
@Composable
private fun PremiumActiveCardPreview() {
    SpeedoTheme {
        PremiumActiveCard()
    }
}

@Preview(showBackground = true, name = "Overlay Card - Enabled")
@Composable
private fun OverlayCardPreview() {
    SpeedoTheme {
        OverlayCard(
            settings = OverlaySettings(enabled = true, opacity = 80),
            onEnabledChange = {},
            onModeChange = {},
            onSizeChange = {},
            onOpacityChange = {},
        )
    }
}
