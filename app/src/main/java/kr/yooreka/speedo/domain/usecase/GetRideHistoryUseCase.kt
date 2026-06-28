package kr.yooreka.speedo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.repository.RideRepository
import javax.inject.Inject

class GetRideHistoryUseCase
    @Inject
    constructor(
        private val rideRepository: RideRepository,
    ) {
        operator fun invoke(): Flow<List<Ride>> = rideRepository.observeRides()
    }
