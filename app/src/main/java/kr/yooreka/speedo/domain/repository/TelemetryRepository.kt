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

    /**
     * 주행 종료 예상 감지(F-18). 기록 중 저속(정지/도보 수준)이 설정 시간(F-18a) 동안 지속되면 true.
     * 사용자가 [continueRide] 하거나 속도가 회복되면 false 로 돌아간다.
     */
    val autoStopSuggested: StateFlow<Boolean>

    fun startTelemetry()

    fun stopTelemetry()

    fun startRecording()

    fun stopRecording()

    /**
     * 종료 예상 알림/다이얼로그에서 '종료'를 선택했을 때(F-18). 저속이 시작된 정차 시점 기준으로
     * 이후 데이터를 잘라내어(Trim, §4.1) 저장한다 — 도보 이동/정차 구간이 주행 시간·경로·통계에 포함되지 않게 한다.
     */
    fun confirmAutoStop()

    /** 종료 예상 알림/다이얼로그에서 '계속'을 선택했을 때: 감지 타이머를 초기화한다(F-18). */
    fun continueRide()

    fun flushBuffer()
}
