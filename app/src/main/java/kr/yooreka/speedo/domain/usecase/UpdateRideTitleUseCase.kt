package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.data.local.dao.RideDao
import javax.inject.Inject

/**
 * 주행 기록의 제목을 수정한다.
 */
class UpdateRideTitleUseCase @Inject constructor(
    private val rideDao: RideDao
) {
    suspend operator fun invoke(rideId: Long, title: String) {
        rideDao.updateTitle(rideId, title)
    }
}
