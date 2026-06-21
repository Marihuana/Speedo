package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 활성 lean 측정 전략(F-03)의 결과를 단일 진입점으로 노출하는 도메인 추상화.
 * 구현체가 설정에 따라 [LeanProvider] 전략을 교체하더라도 소비처는 이 인터페이스에만 의존한다.
 */
interface LeanMeasurement {
    /** 활성 전략의 부호 있는 raw roll(도). 영점 미적용. 데이터 없음은 NaN. */
    val leanStream: StateFlow<Float>

    /** 이미 구동 중인지(영점 보정이 자신이 켠 경우에만 끄도록 판단). */
    val isStarted: Boolean

    fun start()

    fun stop()
}
