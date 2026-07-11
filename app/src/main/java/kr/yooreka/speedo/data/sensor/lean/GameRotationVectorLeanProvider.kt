package kr.yooreka.speedo.data.sensor.lean

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kr.yooreka.speedo.data.sensor.datasource.GameRotationVectorSensor
import kr.yooreka.speedo.domain.model.LeanMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TYPE_GAME_ROTATION_VECTOR(가속도+자이로, 자력계 제외) 기반 lean 전략(F-03).
 * 자기 간섭이 없어 바이크 환경 단기 뱅킹 측정에 유리하다(Gemini 권장).
 */
@Singleton
class GameRotationVectorLeanProvider
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sensor: GameRotationVectorSensor,
    ) : RotationVectorBaseLeanProvider(context, sensor) {
        override val mode = LeanMode.GAME_ROTATION_VECTOR
    }
