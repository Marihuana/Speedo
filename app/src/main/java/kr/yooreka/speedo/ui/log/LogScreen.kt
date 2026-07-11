package kr.yooreka.speedo.ui.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan
import com.google.android.gms.maps.model.TextureStyle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.yooreka.speedo.R
import kr.yooreka.speedo.domain.model.LeanConfidence
import kr.yooreka.speedo.domain.model.RideTelemetry
import kotlin.math.abs

// ── 색상 팔레트 ────────────────────────────────────────────────────────────────
private val ScreenBg = Color(0xFF0D1424)
private val SheetBg = Color(0xFF1A2236)
private val NeonLime = Color(0xFFCCFF00)
private val PrimaryText = Color(0xFFFFFFFF)
private val SecondaryText = Color(0xFF8A98B0)
private val PathColor = Color(0xFF4A5878)
private val BackBtnBg = Color(0xFF000000)

// ── 경로 색상 (F-13a: 뱅킹각 Roll 만으로 결정. 속도는 색상이 아닌 선 모양으로 표기) ───────────
// Dark 모드 (어두운 계열)
private val LeanDark15 = Color(0xFF15803D) // Lean < 15°
private val LeanDark30 = Color(0xFFB45309) // 15° ≤ Lean < 30°
private val LeanDark45 = Color(0xFF9A3412) // 30° ≤ Lean < 45°
private val LeanDark45Plus = Color(0xFF7F1D1D) // Lean ≥ 45°

// Light 모드 (High-Saturation Contrast)
private val LeanLight15 = Color(0xFF065F46) // Lean < 15°
private val LeanLight30 = Color(0xFF1E40AF) // 15° ≤ Lean < 30°
private val LeanLight45 = Color(0xFF991B1B) // 30° ≤ Lean < 45°
private val LeanLight45Plus = Color(0xFF4C1D95) // Lean ≥ 45°

/**
 * 지도/상세 렌더용 보정 뱅킹각(F-03b, PRD §4.1).
 * - `OUTLIER_NOISE`: 0°로 평탄화(경로 오염 방지, 초록/수평).
 * - 그 외(VALID/LOW_SPEED_UNRELIABLE): 저장된 보정값 그대로(LOW_SPEED 는 이미 ±15° 클램프되어 저장됨).
 */
private fun renderRoll(point: RideTelemetry): Float = if (point.leanConfidence == LeanConfidence.OUTLIER_NOISE) 0f else point.roll

/** F-13a: 뱅킹각(절대값)만으로 경로 색상을 결정한다. 모드(Light/Dark)별 팔레트를 사용한다. */
private fun routeColor(
    roll: Float,
    isDark: Boolean,
): Color {
    val lean = abs(roll)
    return when {
        lean < 15f -> if (isDark) LeanDark15 else LeanLight15
        lean < 30f -> if (isDark) LeanDark30 else LeanLight30
        lean < 45f -> if (isDark) LeanDark45 else LeanLight45
        else -> if (isDark) LeanDark45Plus else LeanLight45Plus
    }
}

/** F-13a: 속도 구간 → 선 모양(삼각형 스탬프 밀도). */
enum class SpeedStyle { SOLID, SPARSE, DENSE }

private fun speedStyle(speed: Float): SpeedStyle =
    when {
        speed < 100f -> SpeedStyle.SOLID
        speed < 200f -> SpeedStyle.SPARSE
        else -> SpeedStyle.DENSE
    }

/** 속도 모양별 스탬프 텍스처 묶음. 희소=단일 삼각형(ic_path_arrow), 밀집=fast-forward(ic_path_arrow_fast). */
private class SpeedStamps(
    val sparse: TextureStyle,
    val dense: TextureStyle,
)

/**
 * 방향 스탬프 텍스처를 만든다. drawable 을 [widthPx]×[heightPx] 로 렌더하고 아래에 [padBelowPx] 만큼
 * 여백을 둬 스탬프 반복 간격(밀도)을 만든 뒤, **세로로 뒤집어** 화살표가 주행 진행 방향을 가리키게 한다
 * (Maps TextureStyle 기본 방향이 역방향이라 반전한다). 호출 전 [MapsInitializer] 초기화 필요.
 */
private fun directionStamp(
    context: android.content.Context,
    drawableRes: Int,
    widthPx: Int,
    heightPx: Int,
    padBelowPx: Int,
): TextureStyle {
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, drawableRes)!!
    val bitmap =
        android.graphics.Bitmap.createBitmap(
            widthPx,
            heightPx + padBelowPx,
            android.graphics.Bitmap.Config.ARGB_8888,
        )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, widthPx, heightPx)
    drawable.draw(canvas)
    // Maps 의 텍스처 기본 진행 방향과 반대라, 비트맵을 상하 반전해 화살표가 주행 방향을 향하게 한다.
    val flipped =
        android.graphics.Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            android.graphics.Matrix().apply { postScale(1f, -1f) },
            false,
        )
    return TextureStyle.newBuilder(BitmapDescriptorFactory.fromBitmap(flipped)).build()
}

/** 지도에 그릴 경로 점: 위치 + 구간 색상(뱅킹각) + 구간 선 모양(속도) */
@Immutable
data class RoutePoint(
    val position: LatLng,
    val color: Color,
    val speedStyle: SpeedStyle,
)

@Composable
fun LogScreen(
    viewModel: LogViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 잘못된/삭제된 rideId 접근 또는 상세 조회 실패(PRD §3.2 error_invalid_ride)는 지도 대신 에러 화면을 노출한다.
    if (state.isError) {
        LogErrorContent(onBackClick = onBackClick, modifier = modifier)
        return
    }

    val isDarkTheme = isSystemInDarkTheme()
    val routePoints =
        remember(state.routePoints, isDarkTheme) {
            state.routePoints.mapNotNull {
                if (it.latitude != null && it.longitude != null) {
                    RoutePoint(
                        position = LatLng(it.latitude!!, it.longitude!!),
                        color = routeColor(renderRoll(it), isDarkTheme),
                        speedStyle = speedStyle(it.speed),
                    )
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
            val r = renderRoll(it)
            RideSummary(
                time = "",
                distanceKm = "",
                topSpeedKmh = it.speed.toInt().toString(),
                maxLeanDeg = abs(r).toInt().toString(),
                leanDirection =
                    if (r < 0f) {
                        "R"
                    } else if (r > 0f) {
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
        onSelectPrevious = { viewModel.selectPrevious() },
        onSelectNext = { viewModel.selectNext() },
        modifier = modifier,
    )
}

/**
 * 잘못된/삭제된 주행 기록 접근 시 표시하는 에러 화면(PRD §3.2 error_invalid_ride, §4.2).
 * 빈 지도 대신 중앙 정렬 안내 문구 + 뒤로가기 버튼만 노출한다.
 */
@Composable
fun LogErrorContent(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(ScreenBg),
    ) {
        // 좌상단 뒤로가기 버튼(정상 화면 헤더와 동일한 스타일).
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
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

        Text(
            text = stringResource(R.string.error_invalid_ride),
            color = SecondaryText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
        )
    }
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
    onSelectPrevious: () -> Unit = {},
    onSelectNext: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(ScreenBg),
        ) {
            // 가로모드 좌측 (6 비율): Google Map 경로 지도 전체 높이로 배치
            Box(
                modifier =
                    Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
            ) {
                MapRouteSection(
                    routePoints = routePoints,
                    selectedLatLng = selectedLatLng,
                    onMapClick = onMapClick,
                    modifier = Modifier.fillMaxSize(),
                )

                // 지도 위 좌측 상단에 헤더 얹기
                LogHeader(
                    title = title,
                    date = date,
                    onBackClick = onBackClick,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding(),
                )
            }

            // 가로모드 우측 (4 비율): 주행 정보 요약 및 구간 데이터 제어판 (좌측 경계면에 단일 세로선 렌더링)
            Box(
                modifier =
                    Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .background(SheetBg)
                        .drawBehind {
                            drawLine(
                                color = Color(0x80314158),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = 0.6.dp.toPx(),
                            )
                        }
                        .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                RideSummaryLandscapePanel(
                    summary = summary,
                    segmentSummary = segmentSummary,
                    onSelectPrevious = onSelectPrevious,
                    onSelectNext = onSelectNext,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    } else {
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
                onSelectPrevious = onSelectPrevious,
                onSelectNext = onSelectNext,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun RideSummaryLandscapePanel(
    summary: RideSummary,
    segmentSummary: RideSummary?,
    modifier: Modifier = Modifier,
    onSelectPrevious: () -> Unit = {},
    onSelectNext: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (segmentSummary == null) {
            Text(
                text = stringResource(R.string.session_summary),
                color = SecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 세션 요약 가로모드 맞춤형 2x2 그리드 배열 (반응형 weight(1f) 적용)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryItem(
                        R.drawable.ic_duration,
                        stringResource(R.string.summary_time),
                        summary.time,
                        null,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryItem(
                        R.drawable.ic_distance,
                        stringResource(R.string.summary_dist),
                        summary.distanceKm,
                        "KM",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryItem(
                        R.drawable.ic_monitor,
                        stringResource(R.string.summary_top_speed),
                        summary.topSpeedKmh,
                        "KM/H",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryItem(
                        R.drawable.ic_max_lean,
                        stringResource(R.string.summary_max_lean),
                        "${summary.maxLeanDeg}°",
                        null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.segment_telemetry),
                color = NeonLime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.corner_speed),
                        color = SecondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = segmentSummary.topSpeedKmh,
                            color = PrimaryText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "KM/H",
                            color = SecondaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }

                Box(modifier = Modifier.height(48.dp).width(1.dp).background(Color(0xFF314158)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.log_lean_angle),
                        color = SecondaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (segmentSummary.leanDirection.isNotEmpty()) {
                            Text(
                                text = segmentSummary.leanDirection,
                                color = NeonLime,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 1.dp),
                            )
                        }
                        Text(
                            text = "${segmentSummary.maxLeanDeg}°",
                            color = PrimaryText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }

            // 이전 지점 / 다음 지점 점단위 이동 제어 버튼 배치
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentNavButton(label = stringResource(R.string.segment_prev), onStep = onSelectPrevious, modifier = Modifier.weight(1f))
                SegmentNavButton(label = stringResource(R.string.segment_next), onStep = onSelectNext, modifier = Modifier.weight(1f))
            }
        }
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

        // 지도와 색이 비슷해 가독성이 떨어지므로 반투명 다크 배경을 깔아 타이틀/시간을 분리한다.
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0D1424))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
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

    // 속도 구간(F-13a)을 삼각형 스탬프로 표기하기 위한 텍스처. BitmapDescriptor 사용 전 Maps 초기화가 필요하다.
    val speedStamps =
        remember(context) {
            MapsInitializer.initialize(context)
            SpeedStamps(
                // <200km/h: 단일 삼각형, 희소 간격.
                sparse = directionStamp(context, R.drawable.ic_path_arrow, widthPx = 48, heightPx = 48, padBelowPx = 96),
                // ≥200km/h: fast-forward(삼각형 2개 겹침), 밀집 간격.
                dense = directionStamp(context, R.drawable.ic_path_arrow_fast, widthPx = 48, heightPx = 72, padBelowPx = 16),
            )
        }

    // 연속 동일 스타일(색상 + 속도 모양) 구간을 하나의 StyleSpan 으로 병합(coalesce)해 span 개수를 최소화한다.
    // 각 StyleSpan 의 segment 수 합은 정확히 points.size - 1 이다(구간 = 점 사이 선분).
    val routeSpans =
        remember(routePoints, speedStamps) {
            fun strokeFor(
                argb: Int,
                style: SpeedStyle,
            ): StrokeStyle {
                val builder = StrokeStyle.colorBuilder(argb)
                when (style) {
                    SpeedStyle.SOLID -> Unit
                    SpeedStyle.SPARSE -> builder.stamp(speedStamps.sparse)
                    SpeedStyle.DENSE -> builder.stamp(speedStamps.dense)
                }
                return builder.build()
            }

            val spans = ArrayList<StyleSpan>()
            if (routePoints.size >= 2) {
                var runArgb = routePoints[1].color.toArgb()
                var runStyle = routePoints[1].speedStyle
                var runSegments = 0
                for (i in 0 until routePoints.size - 1) {
                    val segmentArgb = routePoints[i + 1].color.toArgb()
                    val segmentStyle = routePoints[i + 1].speedStyle
                    if (segmentArgb == runArgb && segmentStyle == runStyle) {
                        runSegments++
                    } else {
                        spans.add(StyleSpan(strokeFor(runArgb, runStyle), runSegments.toDouble()))
                        runArgb = segmentArgb
                        runStyle = segmentStyle
                        runSegments = 1
                    }
                }
                spans.add(StyleSpan(strokeFor(runArgb, runStyle), runSegments.toDouble()))
            }
            spans
        }

    val cameraPositionState =
        rememberCameraPositionState {
            if (routePoints.isNotEmpty()) {
                position = CameraPosition.fromLatLngZoom(routePoints.first().position, 13f)
            }
        }

    val firstPos = routePoints.firstOrNull()?.position
    var hasAnimatedToRoute by rememberSaveable(firstPos) { mutableStateOf(false) }

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            if (!hasAnimatedToRoute) {
                val boundsBuilder = LatLngBounds.builder()
                routePoints.forEach { boundsBuilder.include(it.position) }
                val bounds = boundsBuilder.build()
                try {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                    hasAnimatedToRoute = true
                } catch (e: Exception) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(bounds.center, 13f)
                    hasAnimatedToRoute = true
                }
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
                // 화이트 아웃라인(언더레이): 색상 라인보다 약간 두껍게 먼저 그려 밝은 맵에서도 시인성을 높인다(≈0.5px 테두리).
                Polyline(
                    points = routeLatLngs,
                    color = Color.White,
                    width = 15f,
                )
                // 구간별 색상(뱅킹각 Roll) + 선 모양(속도)은 StyleSpan 으로 표현한다.
                // 모든 점을 순서대로 포함하는 단일 Polyline 이라 색 경계에서 끊김이 없다.
                Polyline(
                    points = routeLatLngs,
                    spans = routeSpans,
                    width = 14f,
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
    onSelectPrevious: () -> Unit = {},
    onSelectNext: () -> Unit = {},
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
                text = stringResource(R.string.session_summary),
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
                SummaryItem(
                    R.drawable.ic_duration,
                    stringResource(R.string.summary_time),
                    summary.time,
                    null,
                    modifier = Modifier.weight(1f),
                )
                SummaryItem(
                    R.drawable.ic_distance,
                    stringResource(R.string.summary_dist),
                    summary.distanceKm,
                    "KM",
                    modifier = Modifier.weight(1f),
                )
                SummaryItem(
                    R.drawable.ic_monitor,
                    stringResource(R.string.summary_top_speed),
                    summary.topSpeedKmh,
                    "KM/H",
                    modifier = Modifier.weight(1f),
                )
                SummaryItem(
                    R.drawable.ic_max_lean,
                    stringResource(R.string.summary_max_lean),
                    "${summary.maxLeanDeg}°",
                    null,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.segment_telemetry),
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
                        text = stringResource(R.string.corner_speed),
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
                        text = stringResource(R.string.log_lean_angle),
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

            // 앞/뒤 경로 점 이동(F-13 사용성): 지도를 정확히 탭하지 않아도 점 단위로 이동 가능.
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SegmentNavButton(label = stringResource(R.string.segment_prev), onStep = onSelectPrevious, modifier = Modifier.weight(1f))
                SegmentNavButton(label = stringResource(R.string.segment_next), onStep = onSelectNext, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 길게 누르면 [onStep]을 빠르게 반복 호출하는 초기 지연/반복 간격(ms). */
private const val NAV_HOLD_INITIAL_DELAY_MS = 350L
private const val NAV_HOLD_REPEAT_MS = 60L

@Composable
private fun SegmentNavButton(
    label: String,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier =
            modifier
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1D293D))
                .border(0.6.dp, Color(0xFF314158), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        onStep() // 짧게 탭해도 1회 이동.
                        // 꾹 누르면 초기 지연 후 다음 점으로 빠르게 연속 이동한다.
                        val repeatJob =
                            scope.launch {
                                delay(NAV_HOLD_INITIAL_DELAY_MS)
                                while (isActive) {
                                    onStep()
                                    delay(NAV_HOLD_REPEAT_MS)
                                }
                            }
                        waitForUpOrCancellation()
                        repeatJob.cancel()
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color(0xFFCAD5E2),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
private fun SummaryItem(
    icon: Int,
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
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
    val route = sample.map { RoutePoint(it.first, routeColor(it.third, isDark = true), speedStyle(it.second)) }
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
    val route = sample.map { RoutePoint(it.first, routeColor(it.third, isDark = true), speedStyle(it.second)) }
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
