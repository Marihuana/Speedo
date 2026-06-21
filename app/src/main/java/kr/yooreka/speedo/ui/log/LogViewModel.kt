package kr.yooreka.speedo.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.domain.usecase.GetRideDetailUseCase
import kr.yooreka.speedo.domain.usecase.GetRideTelemetryUseCase
import kr.yooreka.speedo.domain.usecase.InterpolateRoutePathUseCase
import kr.yooreka.speedo.domain.usecase.InterpolateShadowSpeedUseCase
import java.text.SimpleDateFormat
import java.util.Date
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LogState())
        val uiState: StateFlow<LogState> = _uiState.asStateFlow()

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

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
                // GPS 음영(터널) 구간 속도를 진입/진출 앵커 평균속도로 역산(F-13d) → 좌표 보간 전에 적용.
                val speedFilled = interpolateShadowSpeedUseCase(telemetryData)
                // 시간주기(F-13b) 행의 빈 좌표를 보간해 경로 점으로 채운다(F-13c). 최고속도는 원본 기준 유지.
                val routePoints = interpolateRoutePathUseCase(speedFilled)

                getRideDetailUseCase(rideId).fold(
                    onSuccess = { ride ->
                        val maxSpd = telemetryData.maxOfOrNull { it.speed } ?: 0f
                        _uiState.value =
                            LogState(
                                isLoading = false,
                                title = ride.title,
                                date = dateFormatter.format(Date(ride.startTime)),
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

        private fun formatDuration(millis: Long): String {
            val seconds = millis / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
        }
    }
