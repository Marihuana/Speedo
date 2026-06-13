package kr.yooreka.speedo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.model.TelemetryData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import javax.inject.Inject

class GetDashboardTelemetryUseCase
    @Inject
    constructor(
        private val telemetryRepository: TelemetryRepository,
        private val gravityRepo: SensorRepository<GravityData>,
        private val locationRepo: SensorRepository<LocationData>,
        private val calibrationRepository: LeanCalibrationRepository,
    ) {
        operator fun invoke(): Flow<TelemetryData> =
            // 제동 판정은 Repository 가 상시 보유하는 단일 소스(brakeStream)를 구독한다.
            // 대시보드 표시값과 주행 로그 저장값이 동일 소스를 쓰도록 보장하기 위함이다.
            combine(
                telemetryRepository.brakeStream,
                gravityRepo.dataStream,
                locationRepo.dataStream,
                calibrationRepository.offsetDegrees,
            ) { brake, gravity, locationData, offset ->
                TelemetryData(
                    speed = locationData.speed,
                    roll = gravity.calibratedRoll(offset),
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
