package kr.yooreka.speedo.ui.widget

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import kr.yooreka.speedo.domain.model.OverlayMode
import kr.yooreka.speedo.domain.model.OverlaySize
import kr.yooreka.speedo.ui.theme.SpeedoTheme

/**
 * 플로팅 오버레이의 Compose 루트(F-19). 설정에 따라 위젯을 선택하고(F-19a),
 * 크기/투명도를 적용하며(F-19b), 탭/롱클릭 드래그 제스처를 위로 전달한다.
 *
 * @param onTap 위젯 탭 시(앱 포그라운드 복귀, F-19).
 * @param onDrag 롱클릭 후 드래그 시 화면 픽셀 이동량(dx, dy). (F-19b 위치 이동)
 */
@Composable
fun OverlayContent(
    speedKmh: String,
    leanAngle: String,
    isHardBrake: Boolean,
    speedUnit: String,
    mode: OverlayMode,
    size: OverlaySize,
    opacity: Int,
    onTap: () -> Unit,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseDensity = LocalDensity.current
    // 크기 조절(F-19b): Density 배율로 dp/sp를 함께 키워 레이아웃 크기까지 변경한다.
    val scaledDensity =
        remember(size, baseDensity) {
            Density(baseDensity.density * size.scale, baseDensity.fontScale)
        }
    val widgetAlpha = remember(opacity) { (opacity / 100f).coerceIn(0f, 1f) }

    Box(
        modifier =
            modifier
                .alpha(widgetAlpha)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onTap() })
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            when (mode) {
                OverlayMode.SPEEDOMETER ->
                    OverlaySpeedometerWidget(
                        speedKmh = speedKmh,
                        isHardBrake = isHardBrake,
                        speedUnit = speedUnit,
                    )
                OverlayMode.LEAN ->
                    OverlayLeanAngleWidget(
                        leanAngle = leanAngle,
                    )
                OverlayMode.COMBINED ->
                    OverlaySpeedLeanWidget(
                        speedKmh = speedKmh,
                        leanAngle = leanAngle,
                        isHardBrake = isHardBrake,
                        speedUnit = speedUnit,
                    )
            }
        }
    }
}

@Preview(name = "Overlay Content - Combined")
@Composable
private fun OverlayContentPreview() {
    SpeedoTheme {
        OverlayContent(
            speedKmh = "88",
            leanAngle = "20°",
            isHardBrake = false,
            speedUnit = "KM/H",
            mode = OverlayMode.COMBINED,
            size = OverlaySize.MEDIUM,
            opacity = 100,
            onTap = {},
            onDrag = { _, _ -> },
        )
    }
}
