package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kr.yooreka.speedo.domain.model.BrakeDetector.BrakeState

/**
 * 텔레메트리 데이터를 관리하는 레포지토리 인터페이스
 */
interface TelemetryRepository {
    val isRecording: Flow<Boolean>

    /**
     * 가속도 스트림을 상시 누적해 산출하는 최신 제동 판정 상태.
     * 대시보드 표시값과 주행 로그 저장값이 동일 소스를 쓰도록 단일화한다.
     */
    val brakeStream: StateFlow<BrakeState>

    fun startTelemetry()

    fun stopTelemetry()

    fun startRecording()

    fun stopRecording()

    fun flushBuffer()
}
