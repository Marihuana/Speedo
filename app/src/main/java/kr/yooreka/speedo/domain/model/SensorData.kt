package kr.yooreka.speedo.domain.model

/**
 * 가속도 데이터 모델
 */
data class AccelerometerData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestamp: Long = 0L,
)

/**
 * 중력 센서 데이터 모델
 */
data class GravityData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
) {
    /** 센서가 유효한 값을 전달하고 있는지 여부. stop() 시 (0,0,0)으로 초기화된다. */
    fun hasData(): Boolean = x != 0f || y != 0f || z != 0f

    fun calculateRoll(): Float {
        if (!hasData()) return 0f
        val roll = kotlin.math.atan2(x.toDouble(), kotlin.math.sqrt(y * y + z * z.toDouble()))
        return Math.toDegrees(roll).toFloat()
    }

    /**
     * 영점(offset)을 적용한 기울기.
     * 데이터가 없으면(센서 미동작) 0을 반환하여 보정값이 -offset 으로 잘못 표시되는 것을 방지한다.
     */
    fun calibratedRoll(offsetDegrees: Float): Float {
        if (!hasData()) return 0f
        return calculateRoll() - offsetDegrees
    }
}

/**
 * 위치 및 속도 데이터 모델
 */
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float = 0f,
    /** 수평 위치 정확도(미터). 0이면 알 수 없음. 값이 클수록 부정확. */
    val accuracy: Float = 0f,
)

/**
 * 회전 벡터 센서 데이터 모델(TYPE_ROTATION_VECTOR / TYPE_GAME_ROTATION_VECTOR 공용).
 * 단위 쿼터니언 성분(x,y,z,w). roll 산출은 [kr.yooreka.speedo.domain.repository.LeanProvider] 전략에서 수행한다.
 */
data class RotationVectorData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 0f,
    val hasData: Boolean = false,
)

/**
 * 자이로스코프 데이터 모델. 각 축 각속도(rad/s)와 센서 이벤트 타임스탬프(ns, 적분 dt 계산용).
 */
data class GyroscopeData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    /** SensorEvent.timestamp (나노초, 단조 증가). 0이면 데이터 없음. */
    val timestampNanos: Long = 0L,
)

/**
 * 타이어 공기압(TPMS) 데이터 모델
 */
data class TpmsData(
    val frontPressurePsi: Float = 0f,
    val rearPressurePsi: Float = 0f,
    val frontTemperature: Float = 0f,
    val rearTemperature: Float = 0f,
    val frontBatteryVoltage: Float = 0f,
    val rearBatteryVoltage: Float = 0f,
    val timestamp: Long = 0L,
)
