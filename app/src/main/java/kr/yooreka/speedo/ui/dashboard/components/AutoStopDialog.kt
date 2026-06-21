package kr.yooreka.speedo.ui.dashboard.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kr.yooreka.speedo.ui.theme.SlateDark
import kr.yooreka.speedo.ui.theme.SlateSubText

private val DangerRed = Color(0xFFFB2C36)

/**
 * 주행 종료 예상 확인 다이얼로그(F-18). 기록 중 저속이 임계 시간 지속되면 표시한다.
 * [onContinue]=아니오(기록 계속), [onStop]=예(기록 종료).
 */
@Composable
fun AutoStopDialog(
    onContinue: () -> Unit,
    onStop: () -> Unit,
) {
    // 바깥 탭/뒤로가기는 '계속'으로 처리(데이터 유실 방지 — 명시적 '예'에서만 종료).
    Dialog(onDismissRequest = onContinue) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 25.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                    ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0x80314158)),
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 빨간 경고(!) 배지
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .background(DangerRed.copy(alpha = 0.12f), CircleShape)
                            .border(2.dp, DangerRed.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "!",
                        color = DangerRed,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "주행 종료 확인",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.45).sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "주행 종료로 판단됩니다.\n주행 기록을 종료하겠습니까?",
                    color = SlateSubText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    letterSpacing = (-0.15).sp,
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = onContinue,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF314158)),
                    ) {
                        Text(
                            text = "아니오",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                    }

                    Button(
                        onClick = onStop,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    ) {
                        Text(
                            text = "예",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }
        }
    }
}
