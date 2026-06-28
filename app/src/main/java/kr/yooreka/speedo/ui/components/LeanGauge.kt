package kr.yooreka.speedo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.yooreka.speedo.ui.theme.NeonGreen

/** 좌/우 대칭 린앵글 게이지의 한쪽. */
enum class GaugeSide { LEFT, RIGHT }

private const val ARC_SWEEP_TOTAL = 120f
private val DefaultTrackColor = Color(0xFF525468)

/**
 * 한쪽(좌/우) 호형 린앵글 게이지. 대시보드 SpeedometerCard 와 플로팅 오버레이가 공유한다.
 *
 * 좌/우 두 개를 [valueProvider] = `{ lean }` (LEFT) / `{ -lean }` (RIGHT) 로 배치하면
 * 한 번의 부호 규약으로 양쪽 채움이 일관되게 동작한다.
 * 범위([minValue]/[maxValue])·두께([strokeWidth])·여백([edgeInset])·색([trackColor]/[activeColor])을
 * 화면별로 조절할 수 있다.
 */
@Composable
fun LeanGauge(
    minValue: Float,
    maxValue: Float,
    valueProvider: () -> Float,
    side: GaugeSide,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    edgeInset: Dp = 16.dp,
    trackColor: Color = DefaultTrackColor,
    activeColor: Color = NeonGreen,
) {
    Canvas(modifier = modifier) {
        val value = valueProvider()
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
        val fill = ((value - minValue) / range).coerceIn(0f, 1f)

        val sw = strokeWidth.toPx()
        val edgeInsetPx = edgeInset.toPx()

        val maxRadiusByHeight = (size.height * 0.82f) / 2f
        val maxRadiusByWidth = size.width - edgeInsetPx - (sw / 2f)
        val radius = minOf(maxRadiusByHeight, maxRadiusByWidth)
        val arcSize = radius * 2f

        val centerY = size.height * 0.44f
        val topLeftY = centerY - radius

        val (trackStart, centerX) =
            when (side) {
                GaugeSide.LEFT -> 120f to (size.width - edgeInsetPx)
                GaugeSide.RIGHT -> 300f to edgeInsetPx
            }

        val topLeft = Offset(centerX - radius, topLeftY)
        val rect = Size(arcSize, arcSize)

        drawArc(
            color = trackColor,
            startAngle = trackStart,
            sweepAngle = ARC_SWEEP_TOTAL,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )

        if (fill > 0f) {
            val activeSweep = ARC_SWEEP_TOTAL * fill
            val activeStart =
                when (side) {
                    GaugeSide.LEFT -> trackStart
                    GaugeSide.RIGHT -> trackStart + ARC_SWEEP_TOTAL - activeSweep
                }
            drawArc(
                color = activeColor,
                startAngle = activeStart,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
    }
}
