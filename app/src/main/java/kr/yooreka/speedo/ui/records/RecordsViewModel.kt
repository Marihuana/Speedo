package kr.yooreka.speedo.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.yooreka.speedo.domain.usecase.DeleteRideUseCase
import kr.yooreka.speedo.domain.usecase.GetRideHistoryUseCase
import kr.yooreka.speedo.domain.usecase.UpdateRideTitleUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

import kr.yooreka.speedo.data.billing.BillingRepository

data class RecordsState(
    val records: List<RideRecord> = emptyList(),
    val totalDistance: String = "0.0",
    val isAdRemoved: Boolean = false
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val getRideHistoryUseCase: GetRideHistoryUseCase,
    private val updateRideTitleUseCase: UpdateRideTitleUseCase,
    private val deleteRideUseCase: DeleteRideUseCase,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val uiState: StateFlow<RecordsState> = combine(
        getRideHistoryUseCase(),
        billingRepository.isAdRemoved
    ) { rideEntities, isAdRemoved ->
        val records = rideEntities.map { entity ->
            RideRecord(
                id = entity.id,
                title = entity.title,
                date = dateFormatter.format(Date(entity.startTime)),
                duration = formatDuration(entity.duration),
                distance = String.format("%.1f km", entity.totalDistance),
                maxLean = String.format("%.0f°", entity.maxLean),
                topSpeed = String.format("%.0f", entity.maxSpeed)
            )
        }
        val totalDistance = rideEntities.sumOf { it.totalDistance.toDouble() }
        RecordsState(
            records = records,
            totalDistance = String.format("%.1f", totalDistance),
            isAdRemoved = isAdRemoved
        )
    }
    .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecordsState()
        )

    /** 주행 기록 제목을 수정한다. 공백만 입력하면 무시한다. */
    fun renameRide(rideId: Long, newTitle: String) {
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
