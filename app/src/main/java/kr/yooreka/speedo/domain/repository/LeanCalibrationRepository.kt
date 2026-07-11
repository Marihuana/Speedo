package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 기울기(린 앵글) 영점 보정 리포지토리(F-04).
 *
 * 보정값(offset)은 인메모리 [StateFlow]로 노출되며, 프로세스 킬에 대비해 DataStore 에 영속화된다.
 * 앱 시작 시 저장값을 하이드레이트하므로 사용자가 수동 초기화([reset]) 하기 전까지 값이 보존된다.
 */
interface LeanCalibrationRepository {
    /** 현재 영점 보정값(도 단위). 기본값 0f. */
    val offsetDegrees: StateFlow<Float>

    /**
     * 현재 차체 기울기를 영점(0도)으로 설정한다.
     * 중력 센서가 꺼져 있으면 일시적으로 켜서 첫 유효 샘플을 받은 뒤 다시 끈다.
     */
    suspend fun calibrate()

    /** 영점을 초기화한다(offset = 0). 인메모리와 영속 저장을 함께 클리어한다. */
    fun reset()
}
