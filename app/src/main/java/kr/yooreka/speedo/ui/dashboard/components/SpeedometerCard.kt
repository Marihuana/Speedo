package kr.yooreka.speedo.ui.dashboard.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.components.GaugeSide
import kr.yooreka.speedo.ui.components.LeanGauge
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateSubText
import kr.yooreka.speedo.utils.parseLeanAngle
import java.util.Locale
import kotlin.math.abs

// ── 색상 ──────────────────────────────────────────────────────────────────────
private val SpeedTextColor = NeonGreen

// ── 상수 ──────────────────────────────────────────────────────────────────────
private const val MIN_LEAN_ANGLE = -65f
private const val MAX_LEAN_ANGLE = 65f

/**
 * SpeedometerCard
 */
@Composable
fun SpeedometerCard(
    speedKmh: String,
    leanAngle: String,
    modifier: Modifier = Modifier,
    speedUnit: String = "KM/H",
    isRecording: Boolean = false,
    maxLeftRoll: String = "0°",
    maxRightRoll: String = "0°",
    onMarkIssue: (() -> Unit)? = null,
) {
    val speedInt = speedKmh.toIntOrNull() ?: 0
    val leanFloat = parseLeanAngle(leanAngle)

    val clampedLean = leanFloat.coerceIn(-MAX_LEAN_ANGLE, MAX_LEAN_ANGLE)

    // F-03a: 100ms 갱신의 step 끊김을 UI 레이어에서만 보간(roll 원본·기록값 무영향).
    // spring(NoBouncy, Medium ≈400f) → 오버슈트 없이 약 150~250ms 내 수렴(허용 지연 상한 250ms).
    val animatedLean by animateFloatAsState(
        targetValue = clampedLean,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "leanAngle",
    )
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(359.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000),
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B)),
        contentAlignment = Alignment.Center,
    ) {
        val isCompact = maxHeight < 280.dp

        // Figma Font Specs
        val speedFontSize = if (isCompact) 72.sp else 96.sp
        val speedLetterSpacing = if (isCompact) (-3).sp else (-4.8).sp

        val unitFontSize = if (isCompact) 14.sp else 18.sp

        val leanLabelFontSize = if (isCompact) 11.sp else 14.sp
        val leanLabelLetterSpacing = if (isCompact) 1.0.sp else 1.25.sp

        val directionFontSize = if (isCompact) 24.sp else 36.sp
        val directionLetterSpacing = if (isCompact) (-1.7).sp else (-2.6).sp

        val angleValueFontSize = if (isCompact) 40.sp else 60.sp
        val angleValueLetterSpacing = if (isCompact) (-1.8).sp else (-2.7).sp

        val maxLabelFontSize = if (isCompact) 8.sp else 10.sp
        val maxLabelLetterSpacing = if (isCompact) 0.9.sp else 1.1.sp

        val maxValueFontSize = if (isCompact) 20.sp else 30.sp
        val maxValueLetterSpacing = if (isCompact) 0.27.sp else 0.4.sp

        val bottomPadding = if (isCompact) 16.dp else 32.dp
        val edgePadding = if (isCompact) 16.dp else 24.dp
        val contentBottomPadding = if (isCompact) 16.dp else 24.dp

        val gaugeStrokeWidth = if (isCompact) 12.dp else 16.dp
        val gaugeEdgeInset = if (isCompact) 12.dp else 16.dp

        Spacer(Modifier.size(24.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
        ) {
            LeanGauge(
                minValue = MIN_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                valueProvider = { animatedLean },
                side = GaugeSide.LEFT,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                strokeWidth = gaugeStrokeWidth,
                edgeInset = gaugeEdgeInset,
            )
            LeanGauge(
                minValue = MIN_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                valueProvider = { -animatedLean },
                side = GaugeSide.RIGHT,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                strokeWidth = gaugeStrokeWidth,
                edgeInset = gaugeEdgeInset,
            )
        }

        val density = LocalDensity.current
        val speedShadow =
            remember(density) {
                Shadow(
                    color = Color(0x26000000),
                    offset = Offset(0f, with(density) { 4.dp.toPx() }),
                    blurRadius = with(density) { 8.dp.toPx() },
                )
            }

        // 속도 + UNIT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = contentBottomPadding),
        ) {
            AutoSizeText(
                text = speedInt.toString(),
                color = SpeedTextColor,
                minFontSize = 48f,
                maxFontSize = if (isCompact) 72f else 120f,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(shadow = speedShadow),
            )
            Text(
                text = speedUnit,
                color = Color(0xFF90A1B9),
                fontSize = unitFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = if (isCompact) 1.05.sp else 1.36.sp,
                textAlign = TextAlign.Center,
            )
        }

        // 린앵글 (하단 중앙)
        val direction =
            when {
                animatedLean < 0f -> "R"
                animatedLean > 0f -> "L"
                else -> ""
            }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding),
        ) {
            Text(
                text = stringResource(R.string.lean_angle),
                color = SlateSubText,
                fontSize = leanLabelFontSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = leanLabelLetterSpacing,
            )
            Spacer(modifier = Modifier.height(if (isCompact) 0.dp else 2.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (direction.isNotEmpty()) {
                    Text(
                        text = direction,
                        color = Color.White,
                        fontSize = directionFontSize,
                        fontWeight = FontWeight.Black,
                        letterSpacing = directionLetterSpacing,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${abs(animatedLean).toInt()}°",
                    color = Color.White,
                    fontSize = angleValueFontSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = angleValueLetterSpacing,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        // 최대 린앵글 (양쪽 하단 코너)
        if (isRecording) {
            // 좌측 하단: MAX L
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = edgePadding, bottom = bottomPadding),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "MAX L",
                    color = SlateSubText,
                    fontSize = maxLabelFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = maxLabelLetterSpacing,
                )
                Text(
                    text = maxLeftRoll,
                    color = Color.White,
                    fontSize = maxValueFontSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = maxValueLetterSpacing,
                )
            }

            // 우측 하단: MAX R
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = edgePadding, bottom = bottomPadding),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "MAX R",
                    color = SlateSubText,
                    fontSize = maxLabelFontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = maxLabelLetterSpacing,
                )
                Text(
                    text = maxRightRoll,
                    color = Color.White,
                    fontSize = maxValueFontSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = maxValueLetterSpacing,
                )
            }
        }

        // 테스터 진단용 이슈 제보(1.0 보완): 속도 위(상단 중앙)에 배치해 좌우 MAX 뱅킹각을 가리지 않는다.
        if (isRecording && onMarkIssue != null) {
            SmallFloatingActionButton(
                onClick = onMarkIssue,
                containerColor = Color(0xFFFB2C36),
                contentColor = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = edgePadding),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = stringResource(R.string.cd_report_issue),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Speedometer - Neutral")
@Composable
private fun PreviewNeutral() {
    SpeedometerCard(speedKmh = "0", leanAngle = "0°")
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Speedometer - Recording")
@Composable
private fun PreviewRecording() {
    SpeedometerCard(
        speedKmh = "80",
        leanAngle = "15°",
        isRecording = true,
        maxLeftRoll = "24°",
        maxRightRoll = "28°",
    )
}

// ── 가로모드 전용 컴포저블 및 Autosize Text ───────────────────────────────────────────

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minFontSize: Float = 48f,
    maxFontSize: Float = 120f,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
) {
    var fontSizeValue by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier,
        color = if (readyToDraw) color else Color.Transparent,
        fontSize = fontSizeValue.sp,
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = style,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                if (fontSizeValue > minFontSize) {
                    fontSizeValue = (fontSizeValue - 4f).coerceAtLeast(minFontSize)
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        },
    )
}

@Composable
fun SpeedometerOnlyCard(
    speedKmh: String,
    speedUnit: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onMarkIssue: (() -> Unit)?,
) {
    val speedInt = speedKmh.toIntOrNull() ?: 0

    Box(
        modifier =
            modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000),
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            AutoSizeText(
                text = speedInt.toString(),
                color = SpeedTextColor,
                minFontSize = 48f,
                maxFontSize = 120f,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = speedUnit,
                color = Color(0xFF90A1B9),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.36.sp,
                textAlign = TextAlign.Center,
            )
        }

        if (isRecording && onMarkIssue != null) {
            SmallFloatingActionButton(
                onClick = onMarkIssue,
                containerColor = Color(0xFFFB2C36),
                contentColor = Color.White,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter),
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = stringResource(R.string.cd_report_issue),
                )
            }
        }
    }
}

@Composable
fun LeanAngleLandscapeCard(
    leanAngle: String,
    maxLeftRoll: String,
    maxRightRoll: String,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
) {
    val leanFloat = parseLeanAngle(leanAngle)
    val clampedLean = leanFloat.coerceIn(-MAX_LEAN_ANGLE, MAX_LEAN_ANGLE)

    val animatedLean by animateFloatAsState(
        targetValue = clampedLean,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "leanAngle",
    )

    Box(
        modifier =
            modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000),
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
        ) {
            LeanGauge(
                minValue = MIN_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                valueProvider = { animatedLean },
                side = GaugeSide.LEFT,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                strokeWidth = 12.dp,
                edgeInset = 12.dp,
            )
            LeanGauge(
                minValue = MIN_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                valueProvider = { -animatedLean },
                side = GaugeSide.RIGHT,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                strokeWidth = 12.dp,
                edgeInset = 12.dp,
            )
        }

        val direction =
            when {
                animatedLean < 0f -> "R"
                animatedLean > 0f -> "L"
                else -> ""
            }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.lean_angle),
                color = SlateSubText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (direction.isNotEmpty()) {
                    Text(
                        text = direction,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${abs(animatedLean).toInt()}°",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        if (isRecording) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "MAX L",
                    color = SlateSubText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = maxLeftRoll,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "MAX R",
                    color = SlateSubText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = maxRightRoll,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
fun RideStatsLandscapeCard(
    duration: String,
    distance: String,
    speedUnit: String,
    isRecording: Boolean,
    isRecordingActive: Boolean,
    modifier: Modifier = Modifier,
    onRecordToggle: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0x1A000000),
                    spotColor = Color(0x1A000000),
                )
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B))
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.duration).uppercase(Locale.US),
                        color = SlateSubText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = duration,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.distance).uppercase(Locale.US),
                        color = SlateSubText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$distance ${if (speedUnit == "MPH") "mi" else "km"}",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Button(
                onClick = onRecordToggle,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = if (isRecordingActive) Color(0xFFFB2C36) else NeonGreen,
                        contentColor = if (isRecordingActive) Color.White else Color.Black,
                    ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                Icon(
                    painter =
                        painterResource(
                            id = if (isRecordingActive) R.drawable.ic_stop else R.drawable.ic_play,
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text =
                        if (isRecordingActive) {
                            stringResource(
                                R.string.record_button_stop,
                            )
                        } else {
                            stringResource(R.string.record_button_start)
                        },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
