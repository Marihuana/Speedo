package kr.yooreka.speedo.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.ui.theme.DangerRed
import kr.yooreka.speedo.ui.theme.WarningOrange
import kr.yooreka.speedo.ui.theme.WarningYellow

// ── 상수 ──────────────────────────────────────────────────────────────────────
private const val PULSE_PERIOD_MS = 450
private const val MIN_PULSE_ALPHA = 0.4f
private const val FADE_IN_MS = 150
private const val FADE_OUT_MS = 250
private val BADGE_MARGIN = 16.dp
private val BADGE_CORNER_RADIUS = 50.dp
private val ICON_SIZE = 16.dp

/**
 * 급제동 3단계(LIGHT/MODERATE/HARD)의 시각 스펙을 하나의 불변 모델로 캡슐화한다.
 * 단계별 분기를 [styleFor] 한 곳에 모아(전략/팩토리 성격) 분기 폭발과 색상 산발을 막는다.
 */
private data class BrakeWarningStyle(
    val color: Color,
    val onColor: Color,
    val labelRes: Int,
    val borderWidth: Dp,
    val pulsing: Boolean,
)

/** NONE 은 경고 미노출(null)로 매핑한다. */
private fun styleFor(event: BrakeEvent): BrakeWarningStyle? =
    when (event) {
        BrakeEvent.NONE -> null
        BrakeEvent.LIGHT ->
            BrakeWarningStyle(
                color = WarningYellow,
                onColor = Color.Black,
                labelRes = R.string.brake_warning_light,
                borderWidth = 3.dp,
                pulsing = false,
            )
        BrakeEvent.MODERATE ->
            BrakeWarningStyle(
                color = WarningOrange,
                onColor = Color.Black,
                labelRes = R.string.brake_warning_moderate,
                borderWidth = 4.dp,
                pulsing = false,
            )
        BrakeEvent.HARD ->
            BrakeWarningStyle(
                color = DangerRed,
                onColor = Color.White,
                labelRes = R.string.brake_warning_hard,
                borderWidth = 5.dp,
                pulsing = true,
            )
    }

/**
 * F-05: 대시보드 급제동 시각 경고 오버레이.
 *
 * 스피드미터 카드 위에 겹쳐(overlay) 단계별 테두리 + 좌상단 배지로 경고한다.
 * - NONE 이면 아무 것도 차지하지 않아 레이아웃을 밀지 않는다(오버레이 방식).
 * - 급제동은 순간적이므로 나타남/사라짐을 짧은 fade 로 부드럽게 처리한다.
 * - HARD 는 테두리를 펄스(깜빡임)시켜 가장 강하게 강조한다.
 *
 * @param cornerRadius 감싸는 카드와 동일한 라운드 값(테두리를 카드 모서리에 맞추기 위함).
 */
@Composable
fun BrakeWarningOverlay(
    brakeEvent: BrakeEvent,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
) {
    val currentStyle = styleFor(brakeEvent)

    // fade-out 동안에도 색/라벨이 유지되도록 마지막 유효 스타일을 보존한다.
    var lastStyle by remember { mutableStateOf<BrakeWarningStyle?>(null) }
    if (currentStyle != null) lastStyle = currentStyle
    val style = lastStyle ?: return

    Box(modifier) {
        AnimatedVisibility(
            visible = currentStyle != null,
            enter = fadeIn(tween(FADE_IN_MS)),
            exit = fadeOut(tween(FADE_OUT_MS)),
            modifier = Modifier.fillMaxSize(),
        ) {
            BrakeWarningContent(style = style, cornerRadius = cornerRadius)
        }
    }
}

@Composable
private fun BrakeWarningContent(
    style: BrakeWarningStyle,
    cornerRadius: Dp,
) {
    // 펄스는 HARD 에서만 적용하지만, 조건부 컴포저블 호출을 피하기 위해 트랜지션은 항상 구성하고
    // 값만 선택적으로 사용한다(값 계산이라 비용이 낮고 리컴포지션 범위도 넓지 않음).
    val transition = rememberInfiniteTransition(label = "brakePulse")
    val pulse by transition.animateFloat(
        initialValue = MIN_PULSE_ALPHA,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(PULSE_PERIOD_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "brakePulseAlpha",
    )
    val borderAlpha = if (style.pulsing) pulse else 1f
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .border(
                    border = BorderStroke(style.borderWidth, style.color.copy(alpha = borderAlpha)),
                    shape = shape,
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(BADGE_MARGIN)
                    .clip(RoundedCornerShape(BADGE_CORNER_RADIUS))
                    .background(style.color)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = stringResource(R.string.cd_brake_warning),
                tint = style.onColor,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(style.labelRes),
                color = style.onColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
            )
        }
    }
}
