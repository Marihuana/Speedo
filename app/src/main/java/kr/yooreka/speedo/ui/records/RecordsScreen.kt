package kr.yooreka.speedo.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.components.BannerAd
import kr.yooreka.speedo.ui.theme.BackgroundBlack
import kr.yooreka.speedo.ui.theme.DangerRed
import kr.yooreka.speedo.ui.theme.NeonGreen
import kr.yooreka.speedo.ui.theme.SlateDark
import kr.yooreka.speedo.ui.theme.SlateSubText
import kr.yooreka.speedo.ui.theme.SlateText
import kr.yooreka.speedo.ui.theme.SpeedoTheme

data class RideRecord(
    val id: Long,
    val title: String,
    val date: String,
    val duration: String,
    val distance: String,
    val maxLean: String,
    val topSpeed: String,
)

@Composable
fun RecordsScreen(
    viewModel: RecordsViewModel = hiltViewModel(),
    onRecordClick: (Long) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RecordsScreen(
        records = state.records,
        totalDistance = state.totalDistance,
        isAdRemoved = state.isAdRemoved,
        onRecordClick = onRecordClick,
        onRenameRecord = { id, title -> viewModel.renameRide(id, title) },
        onDeleteRecord = { id -> viewModel.deleteRide(id) },
    )
}

@Composable
fun RecordsScreen(
    records: List<RideRecord>,
    totalDistance: String,
    isAdRemoved: Boolean = false,
    onRecordClick: (Long) -> Unit = {},
    onRenameRecord: (Long, String) -> Unit = { _, _ -> },
    onDeleteRecord: (Long) -> Unit = {},
) {
    var editingRecord by remember { mutableStateOf<RideRecord?>(null) }
    var deletingRecord by remember { mutableStateOf<RideRecord?>(null) }

    editingRecord?.let { record ->
        RenameRideDialog(
            currentTitle = record.title,
            onDismiss = { editingRecord = null },
            onConfirm = { newTitle ->
                onRenameRecord(record.id, newTitle)
                editingRecord = null
            },
        )
    }

    deletingRecord?.let { record ->
        DeleteRideDialog(
            title = record.title,
            onDismiss = { deletingRecord = null },
            onConfirm = {
                onDeleteRecord(record.id)
                deletingRecord = null
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundBlack),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
        ) {
            RecordsHeader()

            TotalDistanceCard(totalDistance)

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(records) { record ->
                    RideRecordCard(
                        record = record,
                        onClick = onRecordClick,
                        onRenameClick = { editingRecord = record },
                        onDeleteClick = { deletingRecord = record },
                    )
                }
            }
        }

        if (!isAdRemoved) {
            BannerAd(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .border(0.6.dp, Color(0xFF1E293B))
                        .padding(top = 12.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
fun RecordsHeader() {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.records_title),
            color = NeonGreen,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = stringResource(R.string.records_subtitle),
            color = SlateSubText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
fun TotalDistanceCard(distance: String) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(148.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.total_distance),
                color = SlateText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
            )
            Text(
                text = distance,
                color = NeonGreen,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.35.sp,
            )
            Text(
                text = stringResource(R.string.kilometers),
                color = SlateText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

@Composable
fun RideRecordCard(
    record: RideRecord,
    onClick: (Long) -> Unit,
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth(),
        onClick = { onClick(record.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDark),
        border = androidx.compose.foundation.BorderStroke(1.875.dp, Color.Transparent),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.89).sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.date,
                        color = SlateSubText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = record.distance,
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .clickable { expanded = true },
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier =
                                Modifier
                                    .background(SlateDark)
                                    .border(0.6.dp, Color(0xFF314158), RoundedCornerShape(8.dp)),
                        ) {
                            DropdownMenuItem(
                                text = { Text("타이틀 수정", color = Color.White, fontSize = 14.sp) },
                                onClick = {
                                    expanded = false
                                    onRenameClick()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("삭제", color = DangerRed, fontSize = 14.sp) },
                                onClick = {
                                    expanded = false
                                    onDeleteClick()
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RecordStatItem(iconRes = R.drawable.ic_duration, label = "DURATION", value = record.duration)
                RecordStatItem(iconRes = R.drawable.ic_monitor, label = "TOP SPD", value = record.topSpeed, unit = "KM/H")
                RecordStatItem(iconRes = R.drawable.ic_max_lean, label = "LEAN", value = record.maxLean)
            }
        }
    }
}

@Composable
fun RecordStatItem(
    iconRes: Int,
    label: String,
    value: String,
    unit: String? = null,
) {
    Column(modifier = Modifier.width(86.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = SlateText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                style = androidx.compose.ui.text.TextStyle(textIndent = androidx.compose.ui.text.style.TextIndent(firstLine = 0.sp)),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.15).sp,
            )
            if (unit != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = SlateSubText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.sp,
                    modifier = Modifier.padding(bottom = 1.dp),
                )
            }
        }
    }
}

@Composable
fun RenameRideDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(currentTitle) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF314158)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "타이틀 수정",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.44).sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "주행 기록의 새로운 타이틀을 입력하세요.",
                    color = SlateSubText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.15).sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF45556C),
                            unfocusedBorderColor = Color(0xFF45556C),
                            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                            cursorColor = NeonGreen,
                        ),
                    textStyle =
                        androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            letterSpacing = (-0.31).sp,
                        ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirm(text) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                disabledContainerColor = NeonGreen.copy(alpha = 0.5f),
                            ),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "저장",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.15).sp,
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF314158)),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF45556C)),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "취소",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.15).sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteRideDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF314158)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "주행 기록 삭제",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.44).sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "이 주행 기록을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.",
                    color = SlateSubText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.15).sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB2C36)),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "삭제",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.15).sp,
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF314158)),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF45556C)),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            text = "취소",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.15).sp,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Records Screen Full")
@Composable
fun RecordsScreenPreview() {
    val mockRecords =
        listOf(
            RideRecord(1L, "Sunday Morning Tour", "May 25, 2026", "01:45:22", "64.2 km", "L 38°/R 40°", "185"),
            RideRecord(2L, "Canyon Run", "May 22, 2026", "02:15:18", "87.5 km", "L 42°/R 45°", "203"),
            RideRecord(3L, "Evening Commute", "May 21, 2026", "00:32:45", "18.3 km", "L 28°/R 31°", "92"),
        )
    SpeedoTheme {
        RecordsScreen(records = mockRecords, totalDistance = "368.9", onRecordClick = {})
    }
}

@Preview(showBackground = true, name = "Records Screen Empty")
@Composable
fun RecordsScreenEmptyPreview() {
    SpeedoTheme {
        RecordsScreen(records = emptyList(), totalDistance = "0.0", onRecordClick = {})
    }
}

@Preview
@Composable
fun RideRecordCardPreview() {
    SpeedoTheme {
        RideRecordCard(
            record = RideRecord(1L, "Sunday Morning Tour", "May 25, 2026", "01:45:22", "64.2 km", "L 38°/R 40°", "185"),
            onClick = {},
            onRenameClick = {},
            onDeleteClick = {},
        )
    }
}
