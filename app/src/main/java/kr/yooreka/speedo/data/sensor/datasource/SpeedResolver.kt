package kr.yooreka.speedo.data.sensor.datasource

/**
 * GPS 원시 속도를 표시용 km/h 로 변환하면서 정지 중 노이즈를 억제한다.
 *
 * FusedLocationProvider 는 정지 중에도 GPS 좌표 흔들림으로부터 가짜 속도(수 km/h)를
 * 산출할 수 있다. 다음 두 단계로 이를 걸러낸다:
 * 1. **속도 정확도 게이트**: 속도 정확도를 알 수 있고, 측정 속도가 그 불확실성보다 작거나
 *    같으면 0(정지)으로 본다. 정지 시 측위기는 보통 속도 불확실성을 크게 보고한다.
 * 2. **저속 데드밴드**: [MIN_SPEED_MPS] 미만은 0 으로 본다.
 *
 * 안드로이드 프레임워크에 의존하지 않도록 원시 값만 인자로 받는다.
 */
object SpeedResolver {

    /** 약 2.5km/h. 이 미만은 정지로 간주. */
    const val MIN_SPEED_MPS = 0.7f

    fun toKmh(
        hasSpeed: Boolean,
        speedMps: Float,
        hasSpeedAccuracy: Boolean,
        speedAccuracyMps: Float
    ): Float {
        if (!hasSpeed) return 0f
        if (hasSpeedAccuracy && speedMps <= speedAccuracyMps) return 0f
        if (speedMps < MIN_SPEED_MPS) return 0f
        return speedMps * 3.6f
    }
}
