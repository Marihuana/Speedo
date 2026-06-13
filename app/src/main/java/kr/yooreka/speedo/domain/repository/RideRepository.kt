package kr.yooreka.speedo.domain.repository

import kr.yooreka.speedo.domain.model.Ride
import kr.yooreka.speedo.domain.model.RideTelemetry
import kotlinx.coroutines.flow.Flow

/**
 * 저장된 주행 기록(요약 + 텔레메트리 경로)을 조회/관리하는 레포지토리 인터페이스.
 *
 * 규칙(data_layer.md): 관찰 가능한 스트림은 [Flow], 실패 가능한 단발성 동작은 [Result]로 노출한다.
 */
interface RideRepository {
    /** 전체 주행 기록 목록을 최신순으로 관찰한다. */
    fun observeRides(): Flow<List<Ride>>

    /** 단일 주행 기록을 조회한다. 존재하지 않으면 실패(Result.failure)로 전달한다. */
    suspend fun getRide(rideId: Long): Result<Ride>

    /** 주행의 텔레메트리 경로를 시간순으로 조회한다. */
    suspend fun getRideTelemetry(rideId: Long): Result<List<RideTelemetry>>

    /** 주행 제목을 수정한다. */
    suspend fun updateRideTitle(rideId: Long, title: String): Result<Unit>

    /** 주행 기록과 해당 텔레메트리 로그를 함께 삭제한다. */
    suspend fun deleteRide(rideId: Long): Result<Unit>
}
