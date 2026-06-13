package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * 기울기(린 앵글) 영점 보정 리포지토리.
 *
 * 보정값(offset)은 인메모리 [StateFlow]로 보관되며 **앱 프로세스 종료 시까지 유지**된다.
 * (영구 저장 없음 — 다음 실행 시 0 으로 초기화)
 */
interface LeanCalibrationRepository {

    /** 현재 영점 보정값(도 단위). 기본값 0f. */
    val offsetDegrees: StateFlow<Float>

    /**
     * 현재 차체 기울기를 영점(0도)으로 설정한다.
     * 중력 센서가 꺼져 있으면 일시적으로 켜서 첫 유효 샘플을 받은 뒤 다시 끈다.
     */
    suspend fun calibrate()

    /** 영점을 초기화한다(offset = 0). */
    fun reset()
}
