package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.domain.repository.RideRepository
import javax.inject.Inject

class GetRideTelemetryUseCase
    @Inject
    constructor(
        private val rideRepository: RideRepository,
    ) {
        suspend operator fun invoke(rideId: Long): Result<List<RideTelemetry>> = rideRepository.getRideTelemetry(rideId)
    }
