package kr.yooreka.speedo.domain.model

data class TelemetryData(
    val speed: Float = 0f,
    val roll: Float = 0f,
    val brakeEvent: BrakeEvent = BrakeEvent.NONE,
    val brakeForce: Float = 0f,
    /** 현재 roll 의 신뢰도(F-03b). 대시보드 L/R 최대각은 VALID 일 때만 갱신한다. */
    val leanConfidence: LeanConfidence = LeanConfidence.VALID,
)
