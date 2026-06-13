package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.entity.RideEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRideHistoryUseCase @Inject constructor(
    private val rideDao: RideDao
) {
    operator fun invoke(): Flow<List<RideEntity>> = rideDao.getAllRides()
}
