package kr.yooreka.speedo.ui.settings.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kr.yooreka.speedo.R

@Composable
fun TpmsDisconnectDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .border(
                        width = 0.625.dp,
                        color = Color(0xFF314158).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Warning Icon
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .background(Color(0x80000000), CircleShape)
                            .border(0.625.dp, Color(0xFF314158), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Warning",
                        tint = Color(0xFFFB2C36),
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = stringResource(R.string.dialog_disconnect_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.45).sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = stringResource(R.string.dialog_disconnect_desc),
                    color = Color(0xFF90A1B9),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF314158)),
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.49.sp,
                        )
                    }
                    Button(
                        onClick = onConfirm,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB2C36)),
                    ) {
                        Text(
                            text = stringResource(R.string.confirm),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.49.sp,
                        )
                    }
                }
            }
        }
    }
}
