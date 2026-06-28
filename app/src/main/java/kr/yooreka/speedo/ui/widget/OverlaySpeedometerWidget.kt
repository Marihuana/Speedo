package kr.yooreka.speedo.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateDark
import kr.yooreka.speedo.ui.theme.SlateText
import kr.yooreka.speedo.ui.theme.SpeedoTheme

/**
 * OverlaySpeedometerWidget acts as a compact overlay speedometer and displays
 * a hard braking warning selectively when isHardBrake is true.
 */
@Composable
fun OverlaySpeedometerWidget(
    speedKmh: String,
    isHardBrake: Boolean,
    speedUnit: String = "KM/H",
    modifier: Modifier = Modifier,
) {
    val speedInt = remember(speedKmh) { speedKmh.toIntOrNull() ?: 0 }

    Box(
        modifier =
            modifier
                .size(158.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(SlateDark)
                .border(2.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Red glow overlay when braking
        if (isHardBrake) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            // Red #FA2C36 at 20% opacity
                                            Color(0x33FA2C36),
                                            Color.Transparent,
                                        ),
                                ),
                        ),
            )
        }

        // Center Content: Speed & Unit
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = if (isHardBrake) 16.dp else 0.dp),
        ) {
            Text(
                text = speedInt.toString(),
                color = NeonGreen,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = speedUnit,
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
            )
        }

        // Bottom Content (Brake Warning Badge only when braking)
        if (isHardBrake) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .size(width = 134.dp, height = 27.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(10.dp),
                            clip = false,
                            // Red #FA2C36 at 50% opacity
                            ambientColor = Color(0x80FA2C36),
                            spotColor = Color(0x80FA2C36),
                        )
                        .background(Color(0xFFFA2C36), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.overlay_hard_brake),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0x00000000, name = "Overlay - Normal State")
@Composable
private fun PreviewNormalState() {
    SpeedoTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            OverlaySpeedometerWidget(
                speedKmh = "88",
                isHardBrake = false,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0x00000000, name = "Overlay - Braking State")
@Composable
private fun PreviewBrakingState() {
    SpeedoTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            OverlaySpeedometerWidget(
                speedKmh = "45",
                isHardBrake = true,
            )
        }
    }
}
