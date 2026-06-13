package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.entity.RideEntity
import javax.inject.Inject

class GetRideDetailUseCase @Inject constructor(
    private val rideDao: RideDao
) {
    suspend operator fun invoke(id: Long): RideEntity? {
        return rideDao.getRideById(id)
    }
}
