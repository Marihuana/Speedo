package kr.yooreka.speedo.data.sensor.lean

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
        @ApplicationContext context: Context,
        sensor: RotationVectorSensor,
    ) : RotationVectorBaseLeanProvider(context, sensor) {
        override val mode = LeanMode.ROTATION_VECTOR
    }
