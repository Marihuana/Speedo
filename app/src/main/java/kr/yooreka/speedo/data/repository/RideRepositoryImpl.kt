package kr.yooreka.speedo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kr.yooreka.speedo.data.local.dao.RideDao
import kr.yooreka.speedo.data.local.dao.TelemetryDao
import kr.yooreka.speedo.data.mapper.toDomain
import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.model.RideTelemetry
import kr.yooreka.speedo.domain.repository.RideRepository
import javax.inject.Inject

/**
 * [RideRepository]의 Room 기반 구현체. 저장된 주행 기록을 도메인 모델로 변환해 제공한다.
 * 상태를 보유하지 않으므로(stateless) 별도 스코프를 두지 않는다(di_hilt.md).
 */
class RideRepositoryImpl
    @Inject
    constructor(
        private val rideDao: RideDao,
        private val telemetryDao: TelemetryDao,
    ) : RideRepository {
        override fun observeRides(): Flow<List<Ride>> = rideDao.getAllRides().map { entities -> entities.map { it.toDomain() } }

        override suspend fun getRide(rideId: Long): Result<Ride> =
            runCatching {
                rideDao.getRideById(rideId)?.toDomain()
                    ?: throw NoSuchElementException("Ride not found: id=$rideId")
            }

        override suspend fun getRideTelemetry(rideId: Long): Result<List<RideTelemetry>> =
            runCatching {
                telemetryDao.getTelemetryByRideId(rideId).map { it.toDomain() }
            }

        override suspend fun updateRideTitle(
            rideId: Long,
            title: String,
        ): Result<Unit> =
            runCatching {
                rideDao.updateTitle(rideId, title)
            }

        override suspend fun deleteRide(rideId: Long): Result<Unit> =
            runCatching {
                telemetryDao.deleteByRideId(rideId)
                rideDao.deleteRide(rideId)
            }
    }
