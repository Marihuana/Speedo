package kr.yooreka.speedo.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.components.BannerAd
import kr.yooreka.speedo.ui.settings.components.TpmsConnectionDialog
import kr.yooreka.speedo.ui.settings.components.TpmsDisconnectDialog
import kr.yooreka.speedo.ui.settings.components.TpmsResetDialog
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

    // TPMS(BLE) 권한 요청 런처. 허용되면 보류 중이던 동작(연결 다이얼로그 표시)을 실행한다.
    var pendingTpmsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val blePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            if (grants.values.all { it }) {
                pendingTpmsAction?.invoke()
            } else {
                Toast.makeText(context, context.getString(R.string.ble_permission_required), Toast.LENGTH_SHORT).show()
            }
            pendingTpmsAction = null
        }

    SettingsContent(
        showTpmsData = state.showTpmsData,
        onShowTpmsDataChange = { viewModel.toggleTpms(it) },
        onSaveTpmsIds = { front, rear -> viewModel.saveTpmsIds(front, rear) },
        speedUnit = state.speedUnit,
        onSpeedUnitChange = { viewModel.updateSpeedUnit(it) },
        pressureUnit = state.pressureUnit,
        onPressureUnitChange = { viewModel.updatePressureUnit(it) },
        onResetTpmsIds = {
            viewModel.resetTpmsIds()
            Toast.makeText(context, context.getString(R.string.tpms_ids_reset), Toast.LENGTH_SHORT).show()
        },
        hasSavedTpmsIds = state.hasSavedTpmsIds,
        onEnableTpms = { viewModel.toggleTpms(true) },
        isCalibrating = state.isCalibrating,
        onCalibrateClick = { viewModel.calibrate() },
        onResetCalibration = {
            viewModel.resetCalibration()
            Toast.makeText(context, context.getString(R.string.calibration_reset), Toast.LENGTH_SHORT).show()
        },
        onRequestEnableTpms = { onGranted ->
            if (hasBlePermissions(context)) {
                onGranted()
            } else {
                pendingTpmsAction = onGranted
                blePermissionLauncher.launch(requiredBlePermissions())
            }
        },
        isAdRemoved = state.isAdRemoved,
        onPurchaseRemoveAds = {
            (context as? android.app.Activity)?.let { activity ->
                viewModel.purchaseRemoveAds(activity)
            }
        },
    )
}

/** API 레벨에 맞는 BLE 런타임 권한 목록. */
private fun requiredBlePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        // API 31 미만에서는 BLE 스캔에 위치 권한이 필요하다.
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBlePermissions(context: Context): Boolean =
    requiredBlePermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun SettingsContent(
    showTpmsData: Boolean,
    onShowTpmsDataChange: (Boolean) -> Unit,
    onSaveTpmsIds: (String, String) -> Unit,
    speedUnit: String,
    onSpeedUnitChange: (String) -> Unit,
    pressureUnit: String,
    onPressureUnitChange: (String) -> Unit,
    onResetTpmsIds: () -> Unit = {},
    hasSavedTpmsIds: Boolean = false,
    onEnableTpms: () -> Unit = {},
    isCalibrating: Boolean = false,
    onCalibrateClick: () -> Unit = {},
    onResetCalibration: () -> Unit = {},
    onRequestEnableTpms: (onGranted: () -> Unit) -> Unit = { it() },
    isAdRemoved: Boolean = false,
    onPurchaseRemoveAds: () -> Unit = {},
) {
    var showConnectionDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        TpmsResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                showResetDialog = false
                onResetTpmsIds()
            },
        )
    }

    if (showConnectionDialog) {
        TpmsConnectionDialog(
            onDismiss = { showConnectionDialog = false },
            onConfirm = { frontId, rearId ->
                showConnectionDialog = false
                onSaveTpmsIds(frontId, rearId)
            },
        )
    }

    if (showDisconnectDialog) {
        TpmsDisconnectDialog(
            onDismiss = { showDisconnectDialog = false },
            onConfirm = {
                showDisconnectDialog = false
                onShowTpmsDataChange(false)
            },
        )
    }

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
                showTpmsData = showTpmsData,
                onShowTpmsDataChange = { isChecked ->
                    if (isChecked) {
                        onRequestEnableTpms {
                            if (hasSavedTpmsIds) onEnableTpms() else showConnectionDialog = true
                        }
                    } else {
                        showDisconnectDialog = true
                    }
                },
                onResetTpmsIds = { showResetDialog = true },
                speedUnit = speedUnit,
                onSpeedUnitChange = onSpeedUnitChange,
                pressureUnit = pressureUnit,
                onPressureUnitChange = onPressureUnitChange,
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
    showTpmsData: Boolean,
    onShowTpmsDataChange: (Boolean) -> Unit,
    onResetTpmsIds: () -> Unit,
    speedUnit: String,
    onSpeedUnitChange: (String) -> Unit,
    pressureUnit: String,
    onPressureUnitChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // TPMS Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = "SHOW TPMS DATA",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.71).sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Display tire pressure on dashboard",
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.12.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth(0.9f),
                    )
                }
                Switch(
                    checked = showTpmsData,
                    onCheckedChange = onShowTpmsDataChange,
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

            AnimatedVisibility(
                visible = showTpmsData,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onResetTpmsIds,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(41.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D293D)),
                        border = BorderStroke(0.6.dp, Color(0xFF314158)),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "TPMS SENSOR 정보 초기화",
                            color = Color(0xFFCAD5E2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF314158), thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))

            // Speed Unit Selector
            UnitSelector(
                label = "Speed Unit",
                options = listOf("KM/H", "MPH"),
                selectedOption = speedUnit,
                onOptionSelected = onSpeedUnitChange,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pressure Unit Selector
            UnitSelector(
                label = "Pressure Unit",
                options = listOf("PSI", "BAR"),
                selectedOption = pressureUnit,
                onOptionSelected = onPressureUnitChange,
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
@Preview(showBackground = true, name = "Display Card - Off")
@Composable
fun DisplayCardOffPreview() {
    SpeedoTheme {
        Surface(color = BackgroundBlack, modifier = Modifier.padding(16.dp)) {
            DisplayCard(
                showTpmsData = false,
                onShowTpmsDataChange = {},
                onResetTpmsIds = {},
                speedUnit = "KM/H",
                onSpeedUnitChange = {},
                pressureUnit = "PSI",
                onPressureUnitChange = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Display Card - On")
@Composable
fun DisplayCardOnPreview() {
    SpeedoTheme {
        Surface(color = BackgroundBlack, modifier = Modifier.padding(16.dp)) {
            DisplayCard(
                showTpmsData = true,
                onShowTpmsDataChange = {},
                onResetTpmsIds = {},
                speedUnit = "KM/H",
                onSpeedUnitChange = {},
                pressureUnit = "PSI",
                onPressureUnitChange = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings - Full Screen (All On)")
@Composable
fun SettingsScreenFullPreview() {
    SpeedoTheme {
        SettingsContent(
            showTpmsData = true,
            onShowTpmsDataChange = {},
            onSaveTpmsIds = { _, _ -> },
            speedUnit = "KM/H",
            onSpeedUnitChange = {},
            pressureUnit = "PSI",
            onPressureUnitChange = {},
        )
    }
}
