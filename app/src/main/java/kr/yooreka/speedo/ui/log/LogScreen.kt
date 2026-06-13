package kr.yooreka.speedo.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kr.yooreka.speedo.R
import kotlin.math.abs

// ── 색상 팔레트 ────────────────────────────────────────────────────────────────
private val ScreenBg = Color(0xFF0D1424)
private val SheetBg = Color(0xFF1A2236)
private val NeonLime = Color(0xFFCCFF00)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF8A98B0)
private val PathColor = Color(0xFF4A5878)
private val BackBtnBg = Color(0xFF000000)

// ── 경로 색상 (Lean Angle / Speed 기준) ─────────────────────────────────────────
private val LeanGreen = Color(0xFFCCFF00) // Lean < 15°
private val LeanYellow = Color(0xFFFACC15) // 15° ≤ Lean < 30°
private val LeanOrange = Color(0xFFF97316) // 30° ≤ Lean < 45°
private val LeanRed = Color(0xFFEF4444) // Lean ≥ 45°
private val SpeedBlue = Color(0xFF3B82F6) // Speed ≥ 200 km/h (최우선)

/** 주행 데이터(lean angle, speed)를 경로 색상으로 매핑한다. 속도 200km/h 이상이면 파란색이 우선한다. */
private fun routeColor(
    speed: Float,
    roll: Float,
): Color {
    val lean = abs(roll)
    return when {
        speed >= 200f -> SpeedBlue
        lean < 15f -> LeanGreen
        lean < 30f -> LeanYellow
        lean < 45f -> LeanOrange
        else -> LeanRed
    }
}

/** 지도에 그릴 경로 점: 위치 + 해당 구간 색상 */
data class RoutePoint(
    val position: LatLng,
    val color: Color,
)

@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val routePoints =
        remember(state.routePoints) {
            state.routePoints.mapNotNull {
                if (it.latitude != null && it.longitude != null) {
                    RoutePoint(LatLng(it.latitude!!, it.longitude!!), routeColor(it.speed, it.roll))
                } else {
                    null
                }
            }
        }

    val selectedLatLng =
        remember(state.selectedPoint) {
            val point = state.selectedPoint
            if (point?.latitude != null && point?.longitude != null) LatLng(point.latitude!!, point.longitude!!) else null
        }

    val summary =
        RideSummary(
            time = state.duration,
            distanceKm = state.distance,
            topSpeedKmh = state.maxSpeed,
            maxLeanDeg = state.maxLean,
        )

    val segmentSummary =
        state.selectedPoint?.let {
            RideSummary(
                time = "",
                distanceKm = "",
                topSpeedKmh = it.speed.toInt().toString(),
                maxLeanDeg = abs(it.roll).toInt().toString(),
                leanDirection =
                    if (it.roll < 0f) {
                        "R"
                    } else if (it.roll > 0f) {
                        "L"
                    } else {
                        ""
                    },
            )
        }

    LogScreenContent(
        title = state.title,
        date = state.date,
        routePoints = routePoints,
        selectedLatLng = selectedLatLng,
        summary = summary,
        segmentSummary = segmentSummary,
        onBackClick = onBackClick,
        onMapClick = { latLng ->
            // Find closest point within reasonable distance
            var closestDist = Double.MAX_VALUE
            var closestEntity = state.routePoints.firstOrNull()

            for (entity in state.routePoints) {
                if (entity.latitude != null && entity.longitude != null) {
                    val dx = entity.latitude!! - latLng.latitude
                    val dy = entity.longitude!! - latLng.longitude
                    val dist = dx * dx + dy * dy
                    if (dist < closestDist) {
                        closestDist = dist
                        closestEntity = entity
                    }
                }
            }
            viewModel.selectPoint(closestEntity)
        },
        modifier = modifier,
    )
}

/**
 * 주행 기록 화면.
 */
@Composable
fun LogScreenContent(
    title: String,
    date: String,
    routePoints: List<RoutePoint>,
    selectedLatLng: LatLng?,
    summary: RideSummary,
    segmentSummary: RideSummary?,
    onBackClick: () -> Unit = {},
    onMapClick: (LatLng) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(ScreenBg),
    ) {
        // 지도 + 경로 (상단 80%) — 가장 아래 레이어
        Column(modifier = Modifier.fillMaxSize()) {
            MapRouteSection(
                routePoints = routePoints,
                selectedLatLng = selectedLatLng,
                onMapClick = onMapClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.8f),
            )
            Spacer(modifier = Modifier.weight(0.2f))
        }

        // 헤더 (지도 위에 오버레이)
        LogHeader(
            title = title,
            date = date,
            onBackClick = onBackClick,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding(),
        )

        // 하단 주행정보 시트
        RideSummarySheet(
            summary = summary,
            segmentSummary = segmentSummary,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ── 1) 헤더 ────────────────────────────────────────────────────────────────────

@Composable
fun LogHeader(
    title: String,
    date: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 뒤로가기 버튼
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BackBtnBg)
                    .clickableNoRipple(onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cancel),
                tint = PrimaryText,
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                color = PrimaryText,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = date,
                color = NeonLime,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ── 2) 지도 + 경로 ─────────────────────────────────────────────────────────────

@Composable
fun MapRouteSection(
    routePoints: List<RoutePoint>,
    selectedLatLng: LatLng?,
    onMapClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 모든 경로 점을 가진 단일 Polyline 으로 그려 네이티브 오버레이 수를 1개로 줄인다.
    // 줌/팬 시 엔진이 재투영해야 할 오버레이가 N-1개 → 1개가 되어 쓰로틀링을 제거한다.
    val routeLatLngs = remember(routePoints) { routePoints.map { it.position } }
    // 연속 동일 색상 구간을 하나의 StyleSpan 으로 병합(coalesce)해 span 개수를 최소화한다.
    // 각 StyleSpan 의 segment 수 합은 정확히 points.size - 1 이다(구간 = 점 사이 선분).
    val routeSpans =
        remember(routePoints) {
            val spans = ArrayList<StyleSpan>()
            if (routePoints.size >= 2) {
                var runArgb = routePoints[1].color.toArgb()
                var runSegments = 0
                for (i in 0 until routePoints.size - 1) {
                    val segmentArgb = routePoints[i + 1].color.toArgb()
                    if (segmentArgb == runArgb) {
                        runSegments++
                    } else {
                        spans.add(
                            StyleSpan(StrokeStyle.colorBuilder(runArgb).build(), runSegments.toDouble()),
                        )
                        runArgb = segmentArgb
                        runSegments = 1
                    }
                }
                spans.add(StyleSpan(StrokeStyle.colorBuilder(runArgb).build(), runSegments.toDouble()))
            }
            spans
        }

    val cameraPositionState =
        rememberCameraPositionState {
            if (routePoints.isNotEmpty()) {
                position = CameraPosition.fromLatLngZoom(routePoints.first().position, 13f)
            }
        }

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()
            routePoints.forEach { boundsBuilder.include(it.position) }
            val bounds = boundsBuilder.build()
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            } catch (e: Exception) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(bounds.center, 13f)
            }
        } else {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
                        }
                    }
                } catch (e: SecurityException) {
                    // ignore
                }
            }
        }
    }

    Box(modifier = modifier.background(ScreenBg)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    mapType = MapType.NORMAL,
                    // 과거 기록 리뷰 화면이라 실시간 내 위치 레이어는 불필요(렌더/배터리 절약).
                    isMyLocationEnabled = false,
                ),
            uiSettings =
                MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                ),
            onMapClick = onMapClick,
        ) {
            if (routePoints.size >= 2) {
                // 구간별 색상(lean angle / speed 기준)은 StyleSpan 으로 표현한다.
                // 모든 점을 순서대로 포함하는 단일 Polyline 이라 색 경계에서 끊김이 없다.
                Polyline(
                    points = routeLatLngs,
                    spans = routeSpans,
                    width = 12f,
                )
            }
            selectedLatLng?.let { point ->
                val markerState = remember(point) { MarkerState(position = point) }
                Marker(state = markerState)
            }
        }

        // 하단 안내 문구
        Text(
            text = stringResource(R.string.tap_points_hint),
            color = SecondaryText.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
        )
    }
}

// ── 3) 주행 정보 시트 ───────────────────────────────────────────────────────────

/** 주행 요약 데이터 */
data class RideSummary(
    val time: String,
    val distanceKm: String,
    val topSpeedKmh: String,
    val maxLeanDeg: String,
    val leanDirection: String = "",
)

@Composable
fun RideSummarySheet(
    summary: RideSummary,
    segmentSummary: RideSummary?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SheetBg)
                .border(
                    width = 0.6.dp,
                    color = Color(0x80314158),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 드래그 핸들
        Box(
            modifier =
                Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(20971500.dp))
                    .background(Color(0xFF45556C)),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (segmentSummary == null) {
            Text(
                text = "SESSION SUMMARY",
                color = SecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryItem(R.drawable.ic_duration, "TIME", summary.time, null)
                SummaryItem(R.drawable.ic_distance, "DIST", summary.distanceKm, "KM")
                SummaryItem(R.drawable.ic_monitor, "TOP SPD", summary.topSpeedKmh, "KM/H")
                SummaryItem(R.drawable.ic_max_lean, "MAX LEAN", "${summary.maxLeanDeg}°", null)
            }
        } else {
            Text(
                text = "SEGMENT TELEMETRY",
                color = NeonLime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CORNER SPEED",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = segmentSummary.topSpeedKmh,
                            color = PrimaryText,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.36.sp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "KM/H",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }

                Box(modifier = Modifier.height(64.dp).width(1.dp).background(Color(0xFF314158)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "LEAN ANGLE",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (segmentSummary.leanDirection.isNotEmpty()) {
                            Text(
                                text = segmentSummary.leanDirection,
                                color = NeonLime,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.07.sp,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                        Text(
                            text = "${segmentSummary.maxLeanDeg}°",
                            color = PrimaryText,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.36.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(
    icon: Int,
    label: String,
    value: String,
    unit: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = NeonLime,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.12.sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.15).sp,
            )
            if (unit != null) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = SecondaryText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.21.sp,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

/** 리플 없는 단순 클릭 modifier */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    composed {
        clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    }

@Preview(name = "LogScreen - Session Summary")
@Composable
fun LogScreenPreview() {
    // (위치, speed, roll) 샘플 — 색상 구간을 보여주기 위한 값
    val sample =
        listOf(
            // 초록 (Lean < 15°)
            Triple(LatLng(37.5825, 127.0010), 60f, 5f),
            // 노랑 (15~30°)
            Triple(LatLng(37.5905, 127.0250), 80f, 20f),
            // 주황 (30~45°)
            Triple(LatLng(37.5860, 127.0490), 110f, 38f),
            // 빨강 (≥ 45°)
            Triple(LatLng(37.5650, 127.0530), 130f, 50f),
            // 파랑 (속도 ≥ 200)
            Triple(LatLng(37.5480, 127.0360), 210f, 10f),
            // 노랑
            Triple(LatLng(37.5510, 127.0090), 90f, 25f),
            // 초록
            Triple(LatLng(37.5700, 126.9950), 50f, 3f),
        )
    val route = sample.map { RoutePoint(it.first, routeColor(it.second, it.third)) }
    LogScreenContent(
        title = "SUNDAY MORNING TOUR",
        date = "MAY 25, 2026",
        routePoints = route,
        selectedLatLng = null,
        summary =
            RideSummary(
                time = "01:45",
                distanceKm = "64.2",
                topSpeedKmh = "185",
                maxLeanDeg = "40",
            ),
        segmentSummary = null,
        onBackClick = {},
    )
}

@Preview(name = "LogScreen - Segment Telemetry")
@Composable
fun LogScreenSegmentPreview() {
    val sample =
        listOf(
            Triple(LatLng(37.5825, 127.0010), 60f, 5f),
        )
    val route = sample.map { RoutePoint(it.first, routeColor(it.second, it.third)) }
    LogScreenContent(
        title = "SUNDAY MORNING TOUR",
        date = "MAY 25, 2026",
        routePoints = route,
        selectedLatLng = route[0].position,
        summary = RideSummary("01:45", "64.2", "185", "40"),
        segmentSummary = RideSummary("", "", "75", "51", "R"),
        onBackClick = {},
    )
}
