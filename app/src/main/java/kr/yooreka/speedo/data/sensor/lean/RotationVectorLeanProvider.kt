package kr.yooreka.speedo.data.sensor.lean

import kr.yooreka.speedo.data.sensor.datasource.RotationVectorSensor
import kr.yooreka.speedo.domain.model.LeanMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TYPE_ROTATION_VECTOR(가속도+자이로+자력계 융합) 기반 lean 전략(F-03).
 */
@Singleton
class RotationVectorLeanProvider
    @Inject
    constructor(
        sensor: RotationVectorSensor,
    ) : RotationVectorBaseLeanProvider(sensor) {
        override val mode = LeanMode.ROTATION_VECTOR
    }
