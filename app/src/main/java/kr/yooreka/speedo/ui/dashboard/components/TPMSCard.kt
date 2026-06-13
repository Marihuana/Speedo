package kr.yooreka.speedo.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.theme.*

@Composable
fun TPMSCard(
    rearPressure: String,
    frontPressure: String,
    rearTemp: String,
    frontTemp: String,
    rearBat: String,
    frontBat: String,
    pressureUnit: String = "PSI",
    rearColor: Color = GreenSuccess,
    frontColor: Color = GreenSuccess,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(248.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text(
                text = stringResource(R.string.tpms_status),
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TirePressureBox(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = stringResource(R.string.front),
                    pressure = frontPressure,
                    unit = pressureUnit,
                    temp = frontTemp,
                    bat = frontBat,
                    color = frontColor
                )

                TirePressureBox(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = stringResource(R.string.rear),
                    pressure = rearPressure,
                    unit = pressureUnit,
                    temp = rearTemp,
                    bat = rearBat,
                    color = rearColor
                )
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = GreenSuccess, text = "적정")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = WarningYellow, text = "경고")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = DangerRed, text = "위험")
            }
        }
    }
}

@Composable
fun TirePressureBox(
    modifier: Modifier = Modifier,
    label: String,
    pressure: String,
    unit: String,
    temp: String,
    bat: String,
    color: Color
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .border(1.875.dp, color, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = SlateSubText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.36.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = pressure,
                            color = color,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.07.sp
                        )
                        Text(
                            text = unit,
                            color = SlateSubText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.39.sp
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(0.6.dp).background(SlateDark.copy(alpha=0.5f)))
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TEMP",
                            color = SlateText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.16.sp
                        )
                        Text(
                            text = temp,
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.15).sp
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(SlateDark.copy(alpha=0.5f)))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BAT",
                            color = SlateText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.16.sp
                        )
                        Text(
                            text = bat,
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.15).sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = SlateSubText,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
    }
}

@Preview
@Composable
fun TPMSCardPreview() {
    SpeedoTheme {
        TPMSCard(
            rearPressure = "38.5",
            frontPressure = "36.2",
            rearTemp = "45°",
            frontTemp = "43°",
            rearBat = "2.7V",
            frontBat = "2.8V",
            pressureUnit = "PSI",
            rearColor = GreenSuccess,
            frontColor = GreenSuccess
        )
    }
}

@Preview
@Composable
fun TirePressureBoxPreview() {
    SpeedoTheme {
        TirePressureBox(
            label = "FRONT",
            pressure = "36.2",
            unit = "PSI",
            temp = "43°",
            bat = "2.8V",
            color = GreenSuccess,
            modifier = Modifier.height(150.dp)
        )
    }
}
