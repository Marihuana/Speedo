package kr.yooreka.speedo.domain.model

/**
 * 뱅킹각(roll) 데이터 신뢰도(F-03b, PRD §4.1).
 *
 * 자이로 요율(Yaw Rate) 기반 예측 뱅킹각 검증 모델로 각 샘플의 물리적 타당성을 판정한 결과.
 * Room 에는 enum 이름(TEXT)으로 저장된다.
 */
enum class LeanConfidence {
    /** 예측 뱅킹각과의 오차가 허용치 이내 — 신뢰할 수 있는 측정값. */
    RELIABLE,

    /** 예측 대비 물리적으로 설명 불가한 과대 뱅킹(폰 조작 등) — 예측치로 클리핑 보정됨. */
    OUTLIER_NOISE,

    /** 극저속(< 5km/h)으로 모델 검증 불가 — 원시값을 보존하되 신뢰 낮음으로 표시. */
    LOW_SPEED_UNRELIABLE,
}
