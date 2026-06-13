package kr.yooreka.speedo.domain.model

import kotlin.math.abs

/**
 * 가속도 y축 시계열로부터 제동 이벤트/세기를 판정하는 순수 도메인 로직.
 *
 * 저역통과 필터(EMA)로 노이즈를 줄이고, 직전 표본과의 변화량(Δ)이 임계값을 넘으면
 * 쿨다운 경과 여부를 따져 제동 이벤트로 분류한다. 상태를 누적하므로 [BrakeState] 를
 * 입력/출력으로 받는 리듀서([reduce]) 형태로 표현해 대시보드/기록 양쪽에서 동일하게 공유한다.
 */
object BrakeDetector {
    /** 제동으로 판정하기 시작하는 변화량(Δ) 임계값. */
    const val BRAKE_THRESHOLD = 3.5f

    /** 제동 이벤트 사이의 최소 간격(ms). 연속 이벤트 폭주를 막는다. */
    const val BRAKE_COOLDOWN_MS = 2000L

    /** 저역통과(EMA) 계수. 클수록 raw 값에 민감하게 반응한다. */
    const val ALPHA = 0.8f

    /**
     * 제동 판정 누적 상태. [event]/[force] 는 직전 표본에 대한 판정 결과다.
     */
    data class BrakeState(
        val filteredY: Float = 0f,
        val prevAccelY: Float = 0f,
        val lastBrakeTimeMs: Long = 0L,
        val isFirst: Boolean = true,
        val event: BrakeEvent = BrakeEvent.NONE,
        val force: Float = 0f,
    )

    /**
     * 현재 상태와 새 가속도 표본을 받아 다음 상태를 반환하는 순수 함수(리듀서).
     */
    fun reduce(
        state: BrakeState,
        accel: AccelerometerData,
    ): BrakeState {
        val rawY = accel.y
        if (state.isFirst) {
            return state.copy(
                filteredY = rawY,
                prevAccelY = rawY,
                isFirst = false,
                event = BrakeEvent.NONE,
                force = 0f,
            )
        }

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

        return state.copy(
            filteredY = filtered,
            prevAccelY = filtered,
            lastBrakeTimeMs = lastTime,
            event = event,
            force = delta,
        )
    }
}
