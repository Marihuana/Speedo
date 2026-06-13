package kr.yooreka.speedo.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.scan
import kr.yooreka.speedo.domain.model.AccelerometerData
import kr.yooreka.speedo.domain.model.BrakeEvent
import kr.yooreka.speedo.domain.model.GravityData
import kr.yooreka.speedo.domain.model.LocationData
import kr.yooreka.speedo.domain.model.TelemetryData
import kr.yooreka.speedo.domain.repository.LeanCalibrationRepository
import kr.yooreka.speedo.domain.repository.SensorRepository
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import javax.inject.Inject
import kotlin.math.abs

class GetDashboardTelemetryUseCase
    @Inject
    constructor(
        private val telemetryRepository: TelemetryRepository,
        private val accelRepo: SensorRepository<AccelerometerData>,
        private val gravityRepo: SensorRepository<GravityData>,
        private val locationRepo: SensorRepository<LocationData>,
        private val calibrationRepository: LeanCalibrationRepository,
    ) {
        companion object {
            private const val BRAKE_THRESHOLD = 3.5f
            private const val BRAKE_COOLDOWN_MS = 2000L
            private const val ALPHA = 0.8f
        }

        private data class BrakeState(
            val filteredY: Float = 0f,
            val prevAccelY: Float = 0f,
            val lastBrakeTimeMs: Long = 0L,
            val isFirst: Boolean = true,
            val event: BrakeEvent = BrakeEvent.NONE,
            val force: Float = 0f,
        )

        operator fun invoke(): Flow<TelemetryData> {
            val brakeFlow =
                accelRepo.dataStream.scan(BrakeState()) { state, accel ->
                    val rawY = accel.y
                    if (state.isFirst) {
                        state.copy(
                            filteredY = rawY,
                            prevAccelY = rawY,
                            isFirst = false,
                            event = BrakeEvent.NONE,
                            force = 0f,
                        )
                    } else {
                        val filtered = ALPHA * rawY + (1 - ALPHA) * state.filteredY
                        val delta = abs(filtered - state.prevAccelY)
                        val now = accel.timestamp.takeIf { it > 0 } ?: System.currentTimeMillis()
                        val cooldownPassed = (now - state.lastBrakeTimeMs) > BRAKE_COOLDOWN_MS

                        var event = BrakeEvent.NONE
                        var lastTime = state.lastBrakeTimeMs
                        if (delta >= BRAKE_THRESHOLD && cooldownPassed) {
                            event =
                                when {
                                    delta >= BRAKE_THRESHOLD * 2.0f -> BrakeEvent.HARD
                                    delta >= BRAKE_THRESHOLD * 1.4f -> BrakeEvent.MODERATE
                                    else -> BrakeEvent.LIGHT
                                }
                            lastTime = now
                        }

                        state.copy(
                            filteredY = filtered,
                            prevAccelY = filtered,
                            lastBrakeTimeMs = lastTime,
                            event = event,
                            force = delta,
                        )
                    }
                }

            return combine(
                brakeFlow,
                gravityRepo.dataStream,
                locationRepo.dataStream,
                calibrationRepository.offsetDegrees,
            ) { brakeState, gravity, locationData, offset ->
                val roll = gravity.calibratedRoll(offset)

                TelemetryData(
                    speed = locationData.speed,
                    roll = roll,
                    brakeEvent = brakeState.event,
                    brakeForce = brakeState.force,
                )
            }
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
