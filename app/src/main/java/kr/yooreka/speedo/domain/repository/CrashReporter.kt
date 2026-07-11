package kr.yooreka.speedo.domain.repository

/**
 * 비치명적(Non-fatal) 예외 리포터 추상화(PRD §7.1).
 *
 * 앱을 죽이지 않고 처리(폴백/무시)되는 예외 중 운영상 추적이 필요한 것을 배포 환경에서 수집한다.
 * 도메인/데이터 계층은 이 인터페이스에만 의존하고(DIP), 구체 구현(Firebase)은 data 계층이 제공한다.
 * 구현체는 Firebase 미초기화(google-services.json 부재)나 수집 비활성(debug) 상황에서도
 * 안전하게 no-op 이어야 한다.
 */
interface CrashReporter {
    /**
     * 비치명적 예외를 기록한다.
     *
     * @param throwable 포착된 예외
     * @param message 예외 발생 맥락(선택). 리포트에 부가 로그로 남긴다.
     */
    fun recordNonFatal(
        throwable: Throwable,
        message: String? = null,
    )
}
