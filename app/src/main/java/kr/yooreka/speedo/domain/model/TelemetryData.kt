package kr.yooreka.speedo.domain.model

data class TelemetryData(
    val speed: Float = 0f,
    val roll: Float = 0f,
    val brakeEvent: BrakeEvent = BrakeEvent.NONE,
    val brakeForce: Float = 0f,
    /** 현재 roll 을 최대 뱅킹각 집계에 포함할지(F-03b: 극저속 정지 노이즈/이상치 제외). */
    val countsTowardMax: Boolean = true,
)
