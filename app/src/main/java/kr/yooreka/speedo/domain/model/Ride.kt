package kr.yooreka.speedo.domain.model

/**
 * 주행 기록 요약 도메인 모델. 영속성(Room) 타입과 분리된 순수 Kotlin 모델이다.
 */
data class Ride(
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistance: Float = 0f,
    val maxLean: Float = 0f,
    val maxSpeed: Float = 0f,
    val duration: Long = 0L,
)
