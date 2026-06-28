package kr.yooreka.speedo.domain.usecase

import kr.yooreka.speedo.domain.model.RideTelemetry
import javax.inject.Inject

/**
 * 주행 경로 렌더용 텔레메트리의 빈 좌표를 보간해 채운다(F-13c).
 *
 * 보간 전략은 [PathInterpolator] 로 주입받아 선형/스플라인 교체가 가능하다(Strategy).
 */
class InterpolateRoutePathUseCase
    @Inject
    constructor(
        private val interpolator: PathInterpolator,
    ) {
        operator fun invoke(points: List<RideTelemetry>): List<RideTelemetry> = interpolator.interpolate(points)
    }
