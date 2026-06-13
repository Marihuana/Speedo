package kr.yooreka.speedo.domain.model

data class TelemetryData(
    val speed: Float = 0f,
    val roll: Float = 0f,
    val brakeEvent: BrakeEvent = BrakeEvent.NONE,
    val brakeForce: Float = 0f,
)
