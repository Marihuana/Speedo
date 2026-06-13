package kr.yooreka.speedo.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.yooreka.speedo.data.billing.BillingRepository
import kr.yooreka.speedo.domain.usecase.DeleteRideUseCase
import kr.yooreka.speedo.domain.usecase.GetRideHistoryUseCase
import kr.yooreka.speedo.domain.usecase.UpdateRideTitleUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class RecordsState(
    val records: List<RideRecord> = emptyList(),
    val totalDistance: String = "0.0",
    val isAdRemoved: Boolean = false,
)

@HiltViewModel
class RecordsViewModel
    @Inject
    constructor(
        private val getRideHistoryUseCase: GetRideHistoryUseCase,
        private val updateRideTitleUseCase: UpdateRideTitleUseCase,
        private val deleteRideUseCase: DeleteRideUseCase,
        private val billingRepository: BillingRepository,
    ) : ViewModel() {
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val uiState: StateFlow<RecordsState> =
            combine(
                getRideHistoryUseCase(),
                billingRepository.isAdRemoved,
            ) { rides, isAdRemoved ->
                val records =
                    rides.map { ride ->
                        RideRecord(
                            id = ride.id,
                            title = ride.title,
                            date = dateFormatter.format(Date(ride.startTime)),
                            duration = formatDuration(ride.duration),
                            distance = String.format("%.1f km", ride.totalDistance),
                            maxLean = String.format("%.0f°", ride.maxLean),
                            topSpeed = String.format("%.0f", ride.maxSpeed),
                        )
                    }
                val totalDistance = rides.sumOf { it.totalDistance.toDouble() }
                RecordsState(
                    records = records,
                    totalDistance = String.format("%.1f", totalDistance),
                    isAdRemoved = isAdRemoved,
                )
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = RecordsState(),
                )

        /** 주행 기록 제목을 수정한다. 공백만 입력하면 무시한다. */
        fun renameRide(
            rideId: Long,
            newTitle: String,
        ) {
            val trimmed = newTitle.trim()
            if (trimmed.isEmpty()) return
            viewModelScope.launch {
                updateRideTitleUseCase(rideId, trimmed)
            }
        }

        /** 주행 기록과 해당 텔레메트리 로그를 삭제한다. */
        fun deleteRide(rideId: Long) {
            viewModelScope.launch {
                deleteRideUseCase(rideId)
            }
        }

        private fun formatDuration(millis: Long): String {
            val seconds = millis / 1000
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val remainingSeconds = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
        }
    }
