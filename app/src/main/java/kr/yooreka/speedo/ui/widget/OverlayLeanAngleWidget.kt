package kr.yooreka.speedo.ui.widget

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.components.GaugeSide
import kr.yooreka.speedo.ui.components.LeanGauge
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateDark
import kr.yooreka.speedo.ui.theme.SlateText
import kr.yooreka.speedo.utils.parseLeanAngle
import kotlin.math.abs

private const val MAX_LEAN_ANGLE = 50f // 오버레이 게이지 범위(대시보드와 별도로 조절)
private val OverlayTrackColor = Color(0xFF0F172A)

/**
 * OverlayLeanAngleWidget — 기울기 전용 플로팅 위젯(F-19a).
 * 게이지는 대시보드와 동일한 공용 [LeanGauge](좌/우 한 쌍)를 사용해 방향 규약을 일치시킨다.
 */
@Composable
fun OverlayLeanAngleWidget(
    leanAngle: String,
    modifier: Modifier = Modifier,
) {
    val leanFloat = parseLeanAngle(leanAngle)
    val clampedLean = leanFloat.coerceIn(-MAX_LEAN_ANGLE, MAX_LEAN_ANGLE)

    // F-03a: 100ms 갱신의 step 끊김을 UI 레이어에서만 부드럽게 보간한다.
    val animatedLean by animateFloatAsState(
        targetValue = clampedLean,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "leanAngleSmoothing",
    )

    Box(
        modifier =
            modifier
                .size(160.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(SlateDark)
                .border(
                    width = 1.dp,
                    color = Color(0xFF314158).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp),
                )
                .padding(16.dp),
    ) {
        // 좌상단 라벨
        Text(
            text = stringResource(R.string.overlay_widget_lean),
            color = SlateText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart),
        )

        // 좌/우 대칭 게이지: 대시보드와 동일하게 LEFT={lean}, RIGHT={-lean} 으로 방향 일치.
        Row(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            LeanGauge(
                minValue = -MAX_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                valueProvider = { animatedLean },
                side = GaugeSide.LEFT,
                modifier = Modifier.fillMaxSize().weight(1f),
                strokeWidth = 7.dp,
                edgeInset = 4.dp,
                trackColor = OverlayTrackColor,
            )
            LeanGauge(
                minValue = -MAX_LEAN_ANGLE,
                maxValue = MAX_LEAN_ANGLE,
                valueProvider = { -animatedLean },
                side = GaugeSide.RIGHT,
                modifier = Modifier.fillMaxSize().weight(1f),
                strokeWidth = 7.dp,
                edgeInset = 4.dp,
                trackColor = OverlayTrackColor,
            )
        }

        // 중앙 뱅킹각 표시부. 방향 매핑은 대시보드와 동일: 양수=L, 음수=R.
        val direction =
            when {
                animatedLean > 0f -> "L"
                animatedLean < 0f -> "R"
                else -> ""
            }
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (direction.isNotEmpty()) {
                Text(
                    text = direction,
                    color = NeonGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(end = 4.dp),
                )
            } else {
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = "${abs(animatedLean).toInt()}",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "°",
                color = SlateText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Overlay Lean - Straight")
@Composable
private fun PreviewStraightState() {
    OverlayLeanAngleWidget(leanAngle = "0°")
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Overlay Lean - Left 25")
@Composable
private fun PreviewLeftLeanState() {
    OverlayLeanAngleWidget(leanAngle = "25°")
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Overlay Lean - Right 35")
@Composable
private fun PreviewRightLeanState() {
    OverlayLeanAngleWidget(leanAngle = "-35°")
}
