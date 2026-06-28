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
    /** 뱅킹각 신뢰도(F-03b). 지도/상세 렌더 시 OUTLIER_NOISE 는 0°로 평탄화한다. */
    val leanConfidence: LeanConfidence = LeanConfidence.VALID,
)
