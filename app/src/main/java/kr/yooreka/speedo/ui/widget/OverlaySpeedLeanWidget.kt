package kr.yooreka.speedo.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateDark
import kr.yooreka.speedo.ui.theme.SlateSubText
import kr.yooreka.speedo.ui.theme.SlateText
import kr.yooreka.speedo.ui.theme.SpeedoTheme
import kr.yooreka.speedo.utils.parseLeanAngle
import kotlin.math.abs

/**
 * OverlaySpeedLeanWidget is a composite widget that displays both speedometer (KM/H)
 * and banking angle (LEAN ANGLE) in a single compact overlay container.
 */
@Composable
fun OverlaySpeedLeanWidget(
    speedKmh: String,
    leanAngle: String,
    isHardBrake: Boolean,
    speedUnit: String = "KM/H",
    modifier: Modifier = Modifier,
) {
    val speedValue = remember(speedKmh) { speedKmh.toIntOrNull()?.toString() ?: "0" }

    val leanFloat = remember(leanAngle) { parseLeanAngle(leanAngle) }
    val leanInt = remember(leanFloat) { abs(leanFloat).toInt() }
    val direction =
        remember(leanFloat) {
            // 방향 매핑은 대시보드(SpeedometerCard)와 동일하게 유지: 음수=R, 양수=L.
            when {
                leanFloat < 0f -> "R"
                leanFloat > 0f -> "L"
                else -> ""
            }
        }

    Column(
        modifier =
            modifier
                .size(width = 340.dp, height = 160.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(SlateDark)
                .border(
                    width = 1.dp,
                    color = Color(0xFF314158).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(32.dp),
                )
                .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // 1. 상단 행 (Top Row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 좌측 서비스 타이틀
            Text(
                text = stringResource(R.string.overlay_widget_brand),
                color = NeonGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
            )

            // 우측 급브레이크 경고 배지
            if (isHardBrake) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFB2C36))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.overlay_hard_brake),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }

        // 2. 하단 수치 행 (Content Row)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 좌측 속도 영역
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.overlay_widget_speed),
                    color = SlateSubText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = speedValue,
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp,
                        modifier = Modifier.alignBy(FirstBaseline),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = speedUnit,
                        color = SlateText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignBy(FirstBaseline),
                    )
                }
            }

            // 중앙 세로 분할선
            Box(
                modifier =
                    Modifier
                        .size(width = 1.dp, height = 64.dp)
                        .background(Color(0xFF314158)),
            )

            // 우측 뱅킹각 영역
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(R.string.overlay_widget_lean_angle),
                    color = SlateSubText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.End,
                ) {
                    // 방향 지시자 ("L" / "R")
                    if (direction.isNotEmpty()) {
                        Text(
                            text = direction,
                            color = NeonGreen,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            modifier =
                                Modifier
                                    .alignBy(FirstBaseline)
                                    .padding(end = 4.dp),
                        )
                    } else {
                        // 0도일 때 정렬 유지용 투명 가로 공간 확보
                        Spacer(
                            modifier =
                                Modifier
                                    .width(20.dp)
                                    .alignBy(FirstBaseline),
                        )
                    }

                    // 뱅킹각 숫자
                    Text(
                        text = leanInt.toString(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.4).sp,
                        modifier = Modifier.alignBy(FirstBaseline),
                    )

                    // 도 기호
                    Text(
                        text = "°",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.alignBy(FirstBaseline),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Overlay Speed & Lean - Normal")
@Composable
private fun PreviewNormalState() {
    SpeedoTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            OverlaySpeedLeanWidget(
                speedKmh = "88",
                leanAngle = "15°",
                isHardBrake = false,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Overlay Speed & Lean - Hard Brake")
@Composable
private fun PreviewHardBrakeState() {
    SpeedoTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            OverlaySpeedLeanWidget(
                speedKmh = "45",
                leanAngle = "-25°",
                isHardBrake = true,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F172A, name = "Overlay Speed & Lean - Straight 0")
@Composable
private fun PreviewStraightState() {
    SpeedoTheme {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            OverlaySpeedLeanWidget(
                speedKmh = "0",
                leanAngle = "0°",
                isHardBrake = false,
            )
        }
    }
}
