package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.StateFlow
import kr.yooreka.speedo.domain.model.LeanMode

/**
 * 차체 기울기(lean) 측정 전략의 공통 인터페이스(Strategy, F-03).
 *
 * 구현체는 각자 다른 센서/알고리즘으로 기울기를 산출하지만, 소비처는 [leanStream] 에만 의존하므로
 * 측정 방식 교체에 투명하다. [kr.yooreka.speedo.domain.model.LeanMode] 별로 1:1 대응한다.
 */
interface LeanProvider {
    /** 이 전략이 대응하는 측정 방식. */
    val mode: LeanMode

    /**
     * 부호 있는 raw roll(도, 좌/우 방향 보존). 영점 offset(F-04)·스무딩(F-03a)은 미적용 —
     * 소비처에서 적용하여 전략 교체에 영향받지 않게 한다. 미동작/무효 시 0f.
     */
    val leanStream: StateFlow<Float>

    fun start()

    fun stop()
}
