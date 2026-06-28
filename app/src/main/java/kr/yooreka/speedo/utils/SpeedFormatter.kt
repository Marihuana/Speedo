package kr.yooreka.speedo.utils

/** km/h → mph 환산 계수(F-02). */
const val MPH_PER_KMH = 0.621371f

/** km/h 속도 단위 식별자. */
const val SPEED_UNIT_MPH = "MPH"

/**
 * 설정 단위에 맞춘 표시용 정수 속도(F-02). [unit]이 "MPH"이면 mph로 환산, 그 외에는 km/h.
 * 대시보드와 오버레이가 동일 규칙을 쓰도록 단일 변환점으로 사용한다.
 */
fun displaySpeedInt(
    speedKmh: Float,
    unit: String,
): Int = if (unit == SPEED_UNIT_MPH) (speedKmh * MPH_PER_KMH).toInt() else speedKmh.toInt()

/** 뱅킹각 표시 문자열("N°"). 생산자(ViewModel/Service)와 소비자(위젯/카드)가 같은 규약을 쓰도록 단일화. */
fun formatLeanAngle(rollDegrees: Float): String = "${rollDegrees.toInt()}°"

/** [formatLeanAngle] 로 만든 문자열을 다시 Float 로 파싱. 형식이 깨지면 0f. */
fun parseLeanAngle(text: String): Float = text.removeSuffix("°").toFloatOrNull() ?: 0f
