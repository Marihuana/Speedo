package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.local.entity.TelemetryEntity
import javax.inject.Inject

class GetRideTelemetryUseCase @Inject constructor(
    private val telemetryDao: TelemetryDao
) {
    suspend operator fun invoke(rideId: Long): List<TelemetryEntity> {
        return telemetryDao.getTelemetryByRideId(rideId)
    }
}
