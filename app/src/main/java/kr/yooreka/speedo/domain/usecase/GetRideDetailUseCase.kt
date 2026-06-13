package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.repository.RideRepository
import javax.inject.Inject

class GetRideDetailUseCase @Inject constructor(
    private val rideRepository: RideRepository
) {
    suspend operator fun invoke(id: Long): Result<Ride> = rideRepository.getRide(id)
}
