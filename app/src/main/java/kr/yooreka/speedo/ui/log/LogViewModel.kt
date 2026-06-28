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

        init {
            val rideId = savedStateHandle.get<Long>("rideId") ?: -1L
            if (rideId != -1L) {
                fetchRideDetails(rideId)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false, title = "Error: Invalid Ride ID")
            }
        }

        private fun fetchRideDetails(rideId: Long) {
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
                        _uiState.value = LogState(isLoading = false, title = "Record not found")
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
