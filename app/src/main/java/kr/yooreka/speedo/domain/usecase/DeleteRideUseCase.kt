package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.repository.RideRepository
import javax.inject.Inject

/**
 * 주행 기록과 해당 기록에 속한 텔레메트리 로그를 함께 삭제한다.
 */
class DeleteRideUseCase @Inject constructor(
    private val rideRepository: RideRepository
) {
    suspend operator fun invoke(rideId: Long): Result<Unit> = rideRepository.deleteRide(rideId)
}
