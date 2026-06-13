package kr.yooreka.speedo.ui.dashboard.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.theme.*
import kotlin.math.abs

// ── 색상 ──────────────────────────────────────────────────────────────────────
private val GaugeTrackColor  = Color(0xFF525468)
private val GaugeActiveColor = NeonGreen
private val SpeedTextColor   = NeonGreen

// ── 상수 ──────────────────────────────────────────────────────────────────────
private const val MIN_LEAN_ANGLE  = -65f
private const val MAX_LEAN_ANGLE  = 65f
private const val ARC_SWEEP_TOTAL = 120f

enum class GaugeSide { LEFT, RIGHT }

/**
 * SpeedometerCard
 */
@Composable
fun SpeedometerCard(
    speedKmh: String,
    leanAngle: String,
    speedUnit: String = "KM/H",
    modifier: Modifier = Modifier
) {
    val speedInt = speedKmh.toIntOrNull() ?: 0
    val leanFloat = leanAngle.removeSuffix("°").toFloatOrNull() ?: 0f
    
    val clampedLean = leanFloat.coerceIn(-MAX_LEAN_ANGLE, MAX_LEAN_ANGLE)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(359.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SlateDark, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Spacer(Modifier.size(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            LeanGauge(
                minValue = MIN_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                value = clampedLean,
                side     = GaugeSide.LEFT,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
            LeanGauge(
                minValue = MIN_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                value    = -clampedLean,
                side     = GaugeSide.RIGHT,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }

        // 속도 + UNIT
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Text(
                text          = speedInt.toString(),
                color         = SpeedTextColor,
                fontSize      = 96.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-4.8).sp,
                textAlign     = TextAlign.Center
            )
            Text(
                text          = speedUnit,
                color         = SlateText,
                fontSize      = 18.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.36.sp,
                textAlign     = TextAlign.Center
            )
        }

        // 린앵글 (하단)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text          = stringResource(R.string.lean_angle),
                color         = SlateSubText,
                fontSize      = 12.sp,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            val direction = when {
                clampedLean < 0f -> "R"
                clampedLean > 0f -> "L"
                else             -> ""
            }
            Text(
                text          = if (direction.isEmpty()) "0°" else "$direction ${abs(clampedLean).toInt()}°",
                color         = Color.White,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Black,
                letterSpacing = (-0.45).sp
            )
        }
    }
}

@Composable
fun LeanGauge(
    minValue: Float,
    maxValue: Float,
    value: Float,
    side: GaugeSide,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
) {
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    val fill  = ((value - minValue) / range).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val sw       = strokeWidth.toPx()
        val edgeInset = 16.dp.toPx()

        val maxRadiusByHeight = (size.height * 0.82f) / 2f
        val maxRadiusByWidth  = size.width - edgeInset - (sw / 2f)
        val radius   = minOf(maxRadiusByHeight, maxRadiusByWidth)
        val arcSize  = radius * 2f

        val centerY  = size.height * 0.44f
        val topLeftY = centerY - radius

        val (trackStart, centerX) = when (side) {
            GaugeSide.LEFT  -> 120f to (size.width - edgeInset)
            GaugeSide.RIGHT -> 300f to edgeInset
        }

        val topLeft = Offset(centerX - radius, topLeftY)
        val rect    = Size(arcSize, arcSize)

        drawArc(
            color      = GaugeTrackColor,
            startAngle = trackStart,
            sweepAngle = ARC_SWEEP_TOTAL,
            useCenter  = false,
            topLeft    = topLeft,
            size       = rect,
            style      = Stroke(width = sw, cap = StrokeCap.Round)
        )

        if (fill > 0f) {
            val activeSweep = ARC_SWEEP_TOTAL * fill
            val activeStart = when (side) {
                GaugeSide.LEFT  -> trackStart
                GaugeSide.RIGHT -> trackStart + ARC_SWEEP_TOTAL - activeSweep
            }
            drawArc(
                color      = GaugeActiveColor,
                startAngle = activeStart,
                sweepAngle = activeSweep,
                useCenter  = false,
                topLeft    = topLeft,
                size       = rect,
                style      = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewNeutral() {
    SpeedometerCard(speedKmh = "0", leanAngle = "0°")
}
