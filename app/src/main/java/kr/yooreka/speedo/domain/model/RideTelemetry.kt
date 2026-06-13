package kr.yooreka.speedo.domain.model

/**
 * 저장된 주행 경로의 단일 텔레메트리 지점 도메인 모델.
 * 실시간 대시보드용 [TelemetryData]와 달리 영속 기록 조회/재생에 사용한다.
 */
data class RideTelemetry(
    val id: Long = 0,
    val rideId: Long,
    val timestamp: Long,
    val speed: Float,
    val roll: Float,
    val brakeEvent: BrakeEvent,
    val brakeForce: Float,
    val latitude: Double?,
    val longitude: Double?,
)
