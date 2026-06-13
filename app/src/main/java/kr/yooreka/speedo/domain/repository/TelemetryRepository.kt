package kr.yooreka.speedo.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 텔레메트리 데이터를 관리하는 레포지토리 인터페이스
 */
interface TelemetryRepository {
    val isRecording: Flow<Boolean>
    fun startTelemetry()
    fun stopTelemetry()
    fun startRecording()
    fun stopRecording()
    fun flushBuffer()
}
