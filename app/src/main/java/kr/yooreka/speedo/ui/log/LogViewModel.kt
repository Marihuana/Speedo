package kr.yooreka.speedo.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.yooreka.speedo.di.DefaultDispatcher
import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.domain.usecase.GetRideDetailUseCase
import kr.yooreka.speedo.domain.usecase.GetRideTelemetryUseCase
import kr.yooreka.speedo.domain.usecase.InterpolateRoutePathUseCase
import kr.yooreka.speedo.domain.usecase.InterpolateShadowSpeedUseCase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class LogState(
    val isLoading: Boolean = true,
    val title: String = "",
    val date: String = "",
    val duration: String = "",
    val distance: String = "",
    val maxLean: String = "",
    val maxSpeed: String = "0",
    val routePoints: List<RideTelemetry> = emptyList(),
    val selectedPoint: RideTelemetry? = null,
    // 잘못된/삭제된 rideId 접근 또는 상세 조회 실패 시 에러 화면 노출(PRD §3.2 error_invalid_ride, §4.2).
    val isError: Boolean = false,
)

@HiltViewModel
class LogViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getRideDetailUseCase: GetRideDetailUseCase,
        private val getRideTelemetryUseCase: GetRideTelemetryUseCase,
        private val interpolateShadowSpeedUseCase: InterpolateShadowSpeedUseCase,
        private val interpolateRoutePathUseCase: InterpolateRoutePathUseCase,
        @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LogState())
        val uiState: StateFlow<LogState> = _uiState.asStateFlow()

        // DateTimeFormatter 는 불변·thread-safe (SimpleDateFormat 대체).
        private val dateFormatter =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault())

        // 현재 로드된 주행 id. 마스터-디테일(가로 기록탭)에서 동일 주행 재선택 시 중복 로드를 막는다.
        private var loadedRideId: Long? = null

        init {
            val rideId = savedStateHandle.get<Long>("rideId") ?: -1L
            if (rideId != -1L) {
                fetchRideDetails(rideId)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, isError = true)
            }
        }

        /**
         * 지정한 주행 상세를 로드한다(가로 기록탭 마스터-디테일 우측 패널 진입점).
         * nav 인자로 rideId가 오지 않는 재사용 컨텍스트에서 선택된 주행을 갱신하기 위해 사용한다.
         */
        fun loadRide(rideId: Long) {
            if (rideId == loadedRideId) return
            _uiState.value = LogState(isLoading = true)
            fetchRideDetails(rideId)
        }

        private fun fetchRideDetails(rideId: Long) {
            loadedRideId = rideId
            viewModelScope.launch {
                val telemetryData = getRideTelemetryUseCase(rideId).getOrDefault(emptyList())
                // CPU 집약 보간(F-13c/d)은 Default 디스패처로 오프로딩해 UI/지도 렌더와 경합하지 않게 한다.
                val routePoints =
                    withContext(defaultDispatcher) {
                        // 음영(터널) 구간 속도 역산(F-13d) → 좌표 보간(F-13c) 순서.
                        interpolateRoutePathUseCase(interpolateShadowSpeedUseCase(telemetryData))
                    }

                getRideDetailUseCase(rideId).fold(
                    onSuccess = { ride ->
                        val maxSpd = telemetryData.maxOfOrNull { it.speed } ?: 0f
                        _uiState.value =
                            LogState(
                                isLoading = false,
                                title = ride.title,
                                date = dateFormatter.format(Instant.ofEpochMilli(ride.startTime)),
                                duration = formatDuration(ride.duration),
                                distance = String.format("%.1f", ride.totalDistance),
                                maxLean = String.format("%.0f", ride.maxLean),
                                maxSpeed = String.format("%.0f", maxSpd),
                                routePoints = routePoints,
                                selectedPoint = null,
                            )
                    },
                    onFailure = {
                        _uiState.value = LogState(isLoading = false, isError = true)
                    },
                )
            }
        }

        fun selectPoint(point: RideTelemetry?) {
            _uiState.value = _uiState.value.copy(selectedPoint = point)
        }

        /** 선택 점을 다음 경로 점으로 이동(F-13 사용성). 미선택 시 첫 점을 선택한다. */
        fun selectNext() = moveSelection(1)

        /** 선택 점을 이전 경로 점으로 이동(F-13 사용성). 미선택 시 마지막 점을 선택한다. */
        fun selectPrevious() = moveSelection(-1)

        private fun moveSelection(delta: Int) {
            val points = _uiState.value.routePoints
            if (points.isEmpty()) return
            val current = _uiState.value.selectedPoint
            val nextIndex =
                if (current == null) {
                    if (delta > 0) 0 else points.lastIndex
                } else {
                    (points.indexOf(current) + delta).coerceIn(0, points.lastIndex)
                }
            _uiState.value = _uiState.value.copy(selectedPoint = points[nextIndex])
        }

        private fun formatDuration(millis: Long): String {
            val seconds = millis / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
        }
    }
