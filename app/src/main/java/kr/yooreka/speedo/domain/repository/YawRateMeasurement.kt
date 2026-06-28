package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 차량 yaw(수직축 회전) 요율을 단일 진입점으로 노출하는 도메인 추상화(F-03b).
 *
 * 물리적 뱅킹각 가드가 예측 뱅킹각을 계산하고, 극저속 구간에서 '선회 중' 여부를 판정하는 데 쓴다.
 * 소비처는 측정 방식을 몰라도 되며 이 [yawRateStream] 에만 의존한다.
 */
interface YawRateMeasurement {
    /** 수직축 기준 yaw 요율(rad/s). 데이터 없음은 NaN. */
    val yawRateStream: StateFlow<Float>
}
