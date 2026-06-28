package kr.yooreka.speedo.data.sensor.lean

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * lean(roll) 산출 공통 수학. 모든 전략이 **동일한 부호 규약**을 갖도록, 현행
 * `GravityData.calculateRoll()` 과 같은 공식 `atan2(upX, √(upY²+upZ²))` 을 사용한다.
 * 입력은 "위(up)" 방향 벡터(중력센서/가속도계는 정지 시 위로 +g, 회전벡터는 행렬에서 추출).
 */
internal object LeanMath {
    /** 유효 데이터 없음을 나타내는 sentinel. 소비처는 이를 0(또는 표시 보류)으로 처리한다. */
    const val NO_DATA: Float = Float.NaN

    fun rollFromUpVector(
        upX: Float,
        upY: Float,
        upZ: Float,
    ): Float {
        val roll = atan2(upX.toDouble(), sqrt((upY * upY + upZ * upZ).toDouble()))
        return Math.toDegrees(roll).toFloat()
    }
}
