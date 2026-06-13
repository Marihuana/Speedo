package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import javax.inject.Inject

/**
 * 주행 기록과 해당 기록에 속한 텔레메트리 로그를 함께 삭제한다.
 */
class DeleteRideUseCase @Inject constructor(
    private val rideDao: RideDao,
    private val telemetryDao: TelemetryDao
) {
    suspend operator fun invoke(rideId: Long) {
        telemetryDao.deleteByRideId(rideId)
        rideDao.deleteRide(rideId)
    }
}
