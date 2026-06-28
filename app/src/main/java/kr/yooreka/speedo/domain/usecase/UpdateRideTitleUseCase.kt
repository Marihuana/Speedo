package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.repository.RideRepository
import javax.inject.Inject

/**
 * 주행 기록의 제목을 수정한다.
 */
class UpdateRideTitleUseCase
    @Inject
    constructor(
        private val rideRepository: RideRepository,
    ) {
        suspend operator fun invoke(
            rideId: Long,
            title: String,
        ): Result<Unit> = rideRepository.updateRideTitle(rideId, title)
    }
