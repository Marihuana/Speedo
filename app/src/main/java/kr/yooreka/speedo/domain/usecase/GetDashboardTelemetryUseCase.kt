package kr.yooreka.speedo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.model.TelemetryData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.LeanMeasurement
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import javax.inject.Inject

class GetDashboardTelemetryUseCase
    @Inject
    constructor(
        private val telemetryRepository: TelemetryRepository,
        private val leanMeasurement: LeanMeasurement,
        private val locationRepo: SensorRepository<LocationData>,
        private val calibrationRepository: LeanCalibrationRepository,
    ) {
        operator fun invoke(): Flow<TelemetryData> =
            // 제동 판정은 Repository 가 상시 보유하는 단일 소스(brakeStream)를 구독한다.
            // 대시보드 표시값과 주행 로그 저장값이 동일 소스를 쓰도록 보장하기 위함이다.
            // lean 은 활성 측정 전략(F-03)의 raw roll 에 영점 offset 을 적용한다. NaN(데이터 없음)은 0 처리.
            combine(
                telemetryRepository.brakeStream,
                leanMeasurement.leanStream,
                locationRepo.dataStream,
                calibrationRepository.offsetDegrees,
            ) { brake, lean, locationData, offset ->
                TelemetryData(
                    speed = locationData.speed,
                    roll = if (lean.isNaN()) 0f else lean - offset,
                    brakeEvent = brake.event,
                    brakeForce = brake.force,
                )
            }

        fun start() {
            telemetryRepository.startTelemetry()
        }

        fun stop() {
            telemetryRepository.stopTelemetry()
        }

        fun startRecording() {
            telemetryRepository.startRecording()
        }

        fun stopRecording() {
            telemetryRepository.stopRecording()
        }
    }
