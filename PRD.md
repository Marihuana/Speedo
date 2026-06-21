# PRD: Speedo — 모터사이클 주행 계기판 앱

> 작성: Android-PRD-Builder | 최종 수정: 2026-06-10
> 상태: Confirmed (To-Be 목표 사양) — 제품 결정 항목 전부 확정. 잔여 항목은 TPMS 센서 프로토콜(의도적 "미완성" 명시)뿐.
> 패키지: `kr.yooreka.speedo` | 아키텍처: Clean Architecture(data/domain/ui) + Hilt + Room + Jetpack Compose
> 기준: **To-Be(목표 사양)** — 현재 구현 사실 + 완성 목표를 함께 명시. 미확정 수치는 임의로 채우지 않고 🔲 TBD 로 표기.

---

## 1. Project Overview

### 1.1 목적 (Purpose)
모터사이클 라이더의 스마트폰을 주행 계기판으로 사용하여, 주행 중 다음 4종 지표를 실시간으로 표시하고 주행 단위로 기록·리뷰할 수 있게 한다.
- GPS 기반 속도 (km/h 또는 mph)
- 기기 기울기(roll) 기반 차체 기울기 각도 및 세션 최대 기울기
- 가속도 센서 기반 급제동 이벤트(LIGHT/MODERATE/HARD)
- BLE TPMS 센서 기반 앞/뒤 타이어 공기압·온도·배터리 전압 (※ 단, 센서 데이터 정합성 이슈로 인해 이번 버전에서는 앱 UI 전반에서 비활성화 처리)

### 1.2 타겟 유저 (Target User)
- 1차 타겟: 모터사이클 라이더(스마트폰을 차량에 거치하여 사용).
- 사용 환경: 옥외 주행 중. GPS 수신 가능 환경, 화면 상시 시인 상태.
- 사용 맥락: 주행 시작 시 기록을 켜고, 주행 종료 후 기록 화면에서 주행을 리뷰한다.

### 1.3 수익 모델
- 기본 무료 + 배너/전면 광고(AdMob).
- 인앱 결제 1종 `remove_ads_premium`(소비형 아님, 영구) 구매 시 광고 제거.
  근거: `BillingRepository.kt:24,123-125`
- 프리미엄 잠금 범위는 **광고 제거 전용**으로 확정. 그 외 모든 기능은 무료 사용자에게도 동일 제공한다(기능 게이팅 없음). To-Be에서도 동일 유지.

---

## 2. Core User Flows & Features

표기 규칙: "현재 동작"은 코드 검증 가능 사실, "목표"는 To-Be 추가 요구사항. 정량 수치는 검증 가능 형태로 기술.

### 2.1 화면 구성 / 내비게이션
- 앱 실행 → Splash 화면 표시 후 메인으로 전환. 근거: `MainActivity.kt:85-94`, `SplashScreen.kt`
  - **현재 동작**: Splash 진입 시 런타임 권한 요청(ACCESS_FINE/COARSE_LOCATION, Android 13+ POST_NOTIFICATIONS). 이미 모두 허용 시 요청 생략. 권한 결정(허용/거부 무관) 직후부터 2,000ms 경과 시 메인 전환. 근거: `SplashScreen.kt:50-83`
  - **목표(To-Be) — 권한 게이트 확정**: 위치/알림은 핵심 권한으로 간주한다. 거부 시 메인으로 진행하지 않고, "이 권한이 있어야 앱을 사용할 수 있다"는 안내를 표시한 뒤 앱을 종료한다(허용 시에만 2,000ms 후 메인 진입). 근거: To-Be 확정
    - ⚠️ 검토 권고(객관적): POST_NOTIFICATIONS 거부를 앱 시작 차단 사유로 두는 것은 Android 일반 관행과 다름(알림 없이도 핵심 동작 가능). 본 PRD는 사용자 확정에 따라 알림도 시작 필수로 명세하되, 구현 전 재검토 대상으로 표기.
  - BLE 권한은 Splash에서 요청하지 않는다(F-16/설정에서 요청 — §3.3 참조).
- 메인은 좌우 스와이프 가능한 3개 탭: `Monitor(대시보드)` / `Records(주행 목록)` / `Settings(설정)`. 근거: `MainActivity.kt:111-112,126-130`
- 주행 목록에서 항목 선택 → 해당 주행 상세 로그(`Log`) 화면으로 이동. 근거: `MainActivity.kt:98-105,185-189`
- 뒤로 가기: 메인에서 뒤로 가기 버튼을 2,000ms 이내에 2회 누르면 앱 종료. 1회만 누르면 "한 번 더 누르면 종료" 토스트 표시. 근거: `MainActivity.kt:117-124`

| # | Feature | Input | Output | Expected Behavior (정량적) | 근거 |
|---|---------|-------|--------|---------------------------|------|
| F-01 | 속도 표시 | GPS Location(speed, speedAccuracy) | 정수 속도값 + 단위 | `hasSpeed=false` → 0. `hasSpeedAccuracy && speed≤accuracy` → 0. `speed<0.7 m/s(≈2.5km/h)` → 0. 그 외 `km/h = m/s×3.6`. 표시값은 정수로 절삭. | `SpeedResolver.kt:17,25-28`, `DashBoardViewModel.kt:72-76` |
| F-02 | 속도 단위 변환 | 설정값 speedUnit ∈ {KM/H, MPH} | 변환된 정수 속도 | MPH 선택 시 `mph = km/h × 0.621371` 후 정수 절삭. 기본값 KM/H. | `DashBoardViewModel.kt:72-76`, `UserPreferencesRepository.kt:20,43` |
| F-03 | 차체 기울기 표시 | 선택된 측정 전략의 센서 스트림 + 영점 offset | `roll = 정수°` | ✅ 측정 방식 전략화(구현됨). 정상선회 시 센서융합이 원심가속도를 중력으로 오인해 **피크 lean 이 과소측정**되는 문제(고정 배율로는 해결 불가, §3.2b)에 대응하여 lean 측정을 `LeanProvider` 전략(Strategy)으로 분리하고 설정에서 런타임 선택한다. 전략 5종: **GravityTilt**(현행 `atan2(x,√(y²+z²))`)·**AccelTilt**·**RotationVector**·**GameRotationVector**(자력계 제외)·**Complementary**(자이로 적분+직선구간 중력 drift 보정). 모든 전략은 동일 부호 규약으로 raw roll(부호 보존)을 산출하고, 영점 offset(F-04)은 소비처에서 적용(전략 교체에 투명). 실주행 비교로 최적 방식을 채택하며, 객관 비교용 진단 CSV 로깅 제공(§3.1·§3.3). **표시 스무딩은 F-03a 참조.** | `LeanProvider`/`LeanProviderSelector`/`LeanMeasurement`, `data/sensor/lean/*`, `SensorData.kt` |
| F-03a | 기울기 표시 스무딩 | F-03 `roll`(정수°), 100ms 갱신 | 매끄럽게 보간된 게이지/텍스트 | ✅ 구현됨. 100ms 주기 갱신 시 게이지·수치의 step 끊김을 제거한다. **UI 레이어에서만 스무딩** — `roll` 원본 및 기록값(F-10)에는 무영향. `SpeedometerCard`의 게이지 fill과 하단 텍스트를 Compose `animateFloatAsState`로 보간한다. `animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)`(≈400f) — 오버슈트 없이 약 150~250ms 내 수렴(**허용 지연 상한 250ms**). 하단 방향 텍스트("L/R n°")도 동일 보간값(`animatedLean`)을 정수 절삭해 게이지와 동기 표시한다. 데이터 레이어 저역통과 필터(F-05식)는 **미적용**(지연 이중 누적 방지). | `SpeedometerCard.kt:55-176` |
| F-04 | 기울기 영점 보정 | 사용자의 보정 실행(센서 정지 상태) | offset(도) 저장 | 보정 시 센서 미가동이면 start 후 첫 유효 샘플 대기(타임아웃 2,000ms). 유효 샘플 도착 시 현재 roll을 offset으로 저장, 타임아웃 시 기존 offset 유지. offset은 앱 프로세스 종료 전까지 메모리 유지(영속 아님). | `LeanCalibrationRepositoryImpl.kt:25-56` |
| F-05 | 급제동 감지 | 가속도 y축 시계열 | BrakeEvent(NONE/LIGHT/MODERATE/HARD) + force | 저역통과 `filtered = 0.8×raw + 0.2×prev`. `Δ=|filtered−prev|`. 직전 이벤트 후 2,000ms(쿨다운) 경과 + Δ≥3.5 시: Δ≥7.0→HARD, Δ≥4.9→MODERATE, 그 외→LIGHT. ✅ 구현됨(F-13b): 대시보드 감지에 더해 **주행 로그에도 기록**된다. 200ms 시간주기 행에 `brakeEvent`/`brakeForce`를 동적 저장(`brake.event`/`brake.force`). | `GetDashboardTelemetryUseCase.kt:25-27,51-65`, `TelemetryRepositoryImpl.kt:196,206-207` |
| F-06 | 주행 거리 누적 | GPS 좌표열(lat,lng,accuracy) | 누적 거리(km) | (0,0) 좌표 무시. 정확도>25m 측위 무시. 직전 채택 좌표와의 구간 거리<2m이면 무시(기준점 유지). 그 외 구간 거리를 WGS84(`Location.distanceBetween`)로 누적. 첫 유효 좌표는 기준점만 설정. | `RideDistanceTracker.kt:48-62`, `TelemetryRepositoryImpl.kt:51-58,136,220,223` |
| F-07 | TPMS 표시 | BLE TPMS 광고 패킷(앞/뒤) | 앞/뒤 압력·온도·전압 + 경고색 | **⚠️ 이번 버전 비활성화**: 실제 센서 데이터 신뢰성 검증 미흡으로 인해 대시보드(스피드미터) 및 설정 등 전반적인 앱 UI 상에서 TPMS 관련 노출을 모두 비활성화(숨김)한다. (추후 기능 재도입을 고려하여 백엔드 및 데이터 모델 이식성은 손상되지 않도록 유의) | `DashBoardViewModel.kt:52-101`, `TpmsDataSource.kt:90-151` |
| F-08 | TPMS 경고색 | 현재 압력, 기준압(baseline) | 색상(녹/황/적) | **⚠️ 이번 버전 비활성화**: TPMS 기능 비활성화에 따라 UI에서 노출하지 않는다. | `DashBoardViewModel.kt:52-62,78-80` |
| F-08a | 적정공기압 설정 (목표) | 앞/뒤 기준압 입력 | 저장된 baseline | **⚠️ 이번 버전 비활성화**: TPMS 기능 비활성화에 따라 설정 UI에서 제외한다. | (신규 — To-Be) |
| F-09 | 주행 기록 시작/종료 | 사용자의 기록 토글 | 기록 상태, 저장된 주행 | 미기록 상태에서 토글 → 시작 확인 다이얼로그 표시 → 확인 시 기록 시작. 기록 중 토글 → 즉시 종료. 종료 시 광고 미제거 사용자에게 전면 광고 1회 노출. | `DashBoardViewModel.kt:110-127` |
| F-10 | 기록 영속화 | 기록 중 텔레메트리 스트림 | RideEntity + TelemetryEntity(N건) | 기록 시작 시 RideEntity 1건 생성(제목=날짜 포맷). **현행 결함**: 기록된 뱅킹각이 실제보다 현저히 낮게 저장되는 이슈 발생 (실시간 표시와 기록 데이터 간의 불일치). 버퍼 100건 누적 시 DB flush. **목표**: 기록 정밀도 개선 및 데이터 누락 방지를 통해 해당 이슈를 해결한다. | `TelemetryRepositoryImpl.kt:41-42,89-204` |
| F-11 | 백그라운드 기록 | 기록 시작/종료 | 포그라운드 서비스 알림 | 기록 시작 시 `foregroundServiceType=location` 포그라운드 서비스를 IMPORTANCE_LOW 상시 알림과 함께 구동. 종료 시 서비스/알림 제거. 시스템 재시작 시 START_STICKY. | `RecordingService.kt:19-57`, `AndroidManifest.xml:9,36-40` |
| F-12 | 주행 목록 | (없음) | 주행 요약 리스트 + 총거리 | Records 화면. 주행을 `startTime` 내림차순(최신 우선)으로 표시. 화면 상단에 전체 주행 거리 합계를 `%.1f km`로 표시. 각 카드: 제목, 날짜(`MMM dd, yyyy`), 거리(`%.1f km`), DURATION(`HH:MM:SS`), TOP SPD, LEAN(`%.0f°`). 광고 미제거 사용자에게 하단 배너. 페이지네이션 없음(LazyColumn 전체 로드). | `RecordsViewModel.kt:31-58`, `RecordsScreen.kt:116-128,306-308`, `RideDao.kt:22` |
| F-12a | 주행 최고 속도 표시 | (없음) | TOP SPD (km/h) | ✅ 해결됨. 주행 카드의 TOP SPD는 해당 주행의 실제 최고 속도(`RideEntity.maxSpeed`, km/h)를 표시한다. 기록 중 위치 수신마다 세션 최고 속도를 갱신하고 종료 시 저장한다(maxLean과 동일 패턴). 신규 주행부터 채워지며, 컬럼 추가(버전 3) 이전 주행이 잔존할 경우 0으로 표시. | `RideEntity.kt:14`, `TelemetryRepositoryImpl.kt:139-140,197`, `RecordsViewModel.kt:45`, `RecordsScreen.kt:307` |
| F-13 | 주행 상세 로그 | rideId | 지도 경로 + 요약/구간 텔레메트리 | Log 화면. 상단 80% Google Map에 주행 경로를 구간별 색상 Polyline(width 12)으로 표시, 카메라는 전체 경로 bounds에 맞춤. 하단 시트는 기본 SESSION SUMMARY 표시. 지도 탭 시 가장 가까운 점 선택 → SEGMENT TELEMETRY 표시. **[사용성 보정]**: 경로 마커 터치 시 인식 판정 반경(Touch Target)을 확장 보정하여 사용자가 정확한 좌표를 탭하지 못하더라도 인접한 경로 점이 원활히 탭되도록 조작 편의성을 개선하고, 하단 카드 스와이프 또는 내비게이션 버튼을 추가하여 터치 조작 외에도 손쉽게 앞/뒤 경로 점으로 이동할 수 있도록 유도한다. | `LogViewModel.kt:51-77`, `LogScreen.kt:63-145,389-491` |
| F-13a | 경로 시각화 개선 | speed, roll(부호 있음) | 구간 색상 + 선 스타일 | **뱅킹각(Roll)**: 선의 색상으로 표기 (어두운 계열). <15°: Deep Green(`0xFF15803D`), <30°: Dark Amber(`0xFFB45309`), <45°: Dark Orange(`0xFF9A3412`), ≥45°: Deep Red(`0xFF7F1D1D`).<br>**속도(Speed)**: 선의 스타일(삼각형 스탬프)로 표기. <100km/h: 실선, <200km/h: 희소 삼각형(`ic_path_arrow`), ≥200km/h: 밀집 삼각형. **(화살표 방향 버그)**: 지도 상의 `ic_path_arrow`가 실제 차량 주행 이동 방향과 일치하도록 정방향 정합성을 수정해야 한다. | `LogScreen.kt:62-72,114`, `TelemetryRepositoryImpl.kt:117-122` |
| F-13b | 기울기/제동 시간주기 로깅 | 중력·가속도 센서 스트림 | TelemetryEntity(N건) | ✅ 구현됨(커밋 363e6d2). 텔레메트리 저장을 GPS 콜백 종속에서 분리하여 **200ms(5Hz) 시간주기 타이머**로 기록(기록 정밀도 개선). 저장값은 부호 있는 순간 기울기 및 가속도 기반 제동 데이터를 포함한다. | `TelemetryRepositoryImpl.kt:188-213`(타이머), `:206-207`(제동) |
| F-13c | 경로 위치 보간 | GPS 앵커 + 시간주기 행 | 매끄러운 색상 path | ✅ 구현됨(커밋 72d13b3, Strategy 패턴). 시간주기 행 위치를 `null`로 두고, 렌더 전 `InterpolateRoutePathUseCase`(`PathInterpolator`/`LinearPathInterpolator`)가 **타임스탬프 비례 선형 보간**으로 좌표를 채워 5Hz 기울기 색을 배치한다. `TelemetryEntity.lat/lng` nullable 재활용(스키마 무변경). 미관 옵션 Catmull-Rom은 미구현(추후 확장 여지). | `InterpolateRoutePathUseCase.kt`, `LinearPathInterpolator.kt`, `LogViewModel.kt:51-77` |
| F-13d | 음영 구간 속도 보간 | 음영 진입/진출 전후 GPS 데이터 | 보간된 속도 데이터 | ✅ 구현됨(렌더타임, F-13c와 동일 방식 — DB 무변경). 터널 등 GPS 음영 구간은 path(F-13c)는 그려지나 속도가 0/정체로 남는다. 인접 GPS 앵커 간격이 임계값(3s, 정상 1Hz 초과) 이상이면 음영으로 보고, 두 앵커의 좌표·시간으로 **평균속도를 역산**(`Haversine 거리 ÷ 경과시간 × 3.6`)하여 그 사이 행 속도에 채운다. 앵커 자체 실측 속도는 보존. **DB에는 원본(0 등)이 저장되고 LogScreen 조회 시점에만 보정**(F-13c 좌표 보간 직전 적용). | `InterpolateShadowSpeedUseCase.kt`, `LogViewModel.kt:55-78` |
| F-14 | 주행 제목 수정 | rideId, 새 제목 | 갱신된 title | 입력값 trim 후 빈 문자열이면 무시(저장 안 함). 다이얼로그 저장 버튼은 공백일 때 비활성화. `UPDATE rides SET title` 직접 실행. 최대 길이 제한 없음(단일 행 입력). | `RecordsViewModel.kt:60-67`, `RecordsScreen.kt:421`, `RideDao.kt:19-20`, `UpdateRideTitleUseCase.kt:12` |
| F-15 | 주행 삭제 | rideId | 삭제 결과 | 삭제 확인 다이얼로그("되돌릴 수 없습니다") 확인 시 삭제. cascade는 **애플리케이션 레벨 수동 처리**: telemetry_logs(rideId) 먼저 삭제 후 rides 삭제(DB 외래키 제약 없음). | `RecordsScreen.kt:89-98,488`, `DeleteRideUseCase.kt:14-17`, `TelemetryDao.kt:21-22` |
| F-16 | TPMS 센서 등록/해제 | 앞/뒤 센식별자(MAC 또는 이름) | 저장된 센서 ID, TPMS 표시 ON | **⚠️ 이번 버전 비활성화**: TPMS 기능 비활성화에 따라 설정 UI에서 등록/해제 관련 항목을 노출하지 않는다. | `UserPreferencesRepository.kt:66-81`, `TpmsDataSource.kt:43-69` |
| F-17 | 광고 제거 구매 | 결제 실행 | isAdRemoved=true | `remove_ads_premium` 구매 완료 시 광고 제거 + 미승인 구매 자동 acknowledge. 앱 시작 시 기존 구매 복원 질의. | `BillingRepository.kt:59-135` |
| F-18 | 주행 종료 예상 감지 | 기록 중 속도 스트림, 설정 임계값 | 종료 확인 알림/팝업 | **기록 중 속도가 설정된 시간(T) 동안 연속으로 0(F-01 기준)** 일 경우 '주행 종료 예상' 상태로 간주. <br>1. **알림**: "주행이 종료되었습니까?" 알림 발송. [종료], [계속] 버튼 포함. <br>2. **팝업**: 앱이 포그라운드일 경우 화면에 확인 다이얼로그 표시. <br>[종료] 선택 시 즉시 기록 종료(F-09). [계속] 선택 시 감지 타이머 초기화. 응답 없을 시 기록은 유지하되 알림은 상주. | (신규 — To-Be) |
| F-18a | 종료 감지 임계값 설정 | 사용자 선택 (3/5/10/OFF 분) | 저장된 임계값 | 기본값 5분. 사용자가 설정에서 감지 시간을 선택하거나 기능을 끌 수 있음(OFF). 내부적으로는 분 단위 정수(0=OFF)로 저장. | (신규 — To-Be) |
| F-19 | 플로팅 오버레이 위젯 | 앱 백그라운드 전환, 기록 상태 | 플로팅 위젯 표시 | **기록 중** 앱이 백그라운드로 전환될 때(홈 버튼, 다른 앱 전환 등) 자동으로 활성화되어 다른 앱(내비게이션 등) 위에 표시된다. 앱이 다시 포그라운드로 오면 자동으로 제거된다. | (신규 — To-Be) |
| F-19a | 오버레이 위젯 모드 | 사용자 선택 (3종) | 선택된 모드 위젯 | 설정에서 다음 3가지 중 선택 가능: <br>1. **SPEED+BRAKE**: 현재 속도와 제동 상태 표시. <br>2. **LEAN ONLY**: 현재 뱅킹각(도) 표시. <br>3. **ALL-IN-ONE**: 속도, 제동, 뱅킹각 모두 표시. | (신규 — To-Be) |
| F-19b | 오버레이 위치/투명도 | 드래그(Long Press 후), 설정값 | 조정된 오버레이 | 오버레이를 롱클릭하여 위치를 자유롭게 이동 가능(위치 영구 저장). 설정에서 투명도(30~100%) 및 크기(소/중/대) 조정 가능. | (신규 — To-Be) |
| F-20 | TPMS Raw 데이터 로깅 | 수신된 BLE 광고 패킷 | 외부 저장소 .txt 파일 | **⚠️ 이번 버전 비활성화**: TPMS 기능 전체 비활성화에 따라 UI에서 로깅 옵션 메뉴를 노출하지 않는다. | (신규 — To-Be) |
| F-20a | 로깅 데이터 포맷 | (없음) | 텍스트 로그 행 | **⚠️ 이번 버전 비활성화** | (신규 — To-Be) |

### 2.2 대시보드 갱신 사양
- 텔레메트리 스트림은 100ms 주기로 샘플링하여 UI에 반영. 근거: `DashBoardViewModel.kt:66`
- **기울기(Lean) 표시 스무딩(목표)**: 100ms 주기의 데이터 갱신 시 시각적 끊김을 최소화하기 위해 UI 레벨에서 게이지/수치에 스무딩을 적용한다. 구현 사양(`animateFloatAsState` + `spring`, 허용 지연 상한 250ms, 데이터 레이어 무영향)은 **F-03a** 참조.
- 구독 종료 후 5,000ms간 상태 공유 유지(WhileSubscribed). 근거: `DashBoardViewModel.kt:106`

---

## 3. Data & Technical Requirements

### 3.1 클라이언트 상태 범위 (Client State Scope)
- **화면(휘발)**: `DashBoardState`(표시 문자열, 경고색, 기록 여부 등). 근거: `DashBoardState.kt`
- **프로세스 메모리(앱 종료 시 소멸)**: 기울기 영점 `offsetDegrees`, 기록 세션 누적값(maxLean, 거리, 시작시각). 근거: `LeanCalibrationRepositoryImpl.kt:22`, `TelemetryRepositoryImpl.kt:44-46`
- **영구(DataStore)**: showTpmsData, speedUnit, pressureUnit, frontTpmsId, rearTpmsId, launchCount, leanMeasurementMode(F-03 측정 방식), autoStopThreshold, overlayMode, overlayOpacity, overlaySize, overlayX, overlayY, tpmsLoggingEnabled. 근거: `UserPreferencesRepository.kt`
  - 목표 추가: 사용자 설정 적정공기압(앞/뒤 baseline) 영속 키 — 신규 필요. **PSI 기준으로 저장**(입력 단위 무관). 🔲 TBD: 키 네이밍/마이그레이션.
  - 목표 추가: `autoStopThreshold`(분 단위 정수, 기본 5, 0=OFF).
  - 목표 추가: `overlayMode`(0:Speed+Brake, 1:Lean, 2:All), `overlayOpacity`(Float, 0.3~1.0), `overlaySize`(String: SMALL/MEDIUM/LARGE), `overlayX/Y`(Float, 저장된 좌표).
  - 목표 추가: `tpmsLoggingEnabled`(Boolean, 기본 false).
- **영구(Room)**: rides, telemetry_logs 2테이블. 근거: `SpeedoDatabase.kt`

### 3.2 필수 데이터 구조 (Data Structures)
- `RideEntity`(`rides`): id(PK,autoInc), title:String(non-null), startTime:Long, endTime:Long?, totalDistance:Float(km, 기본0), maxLean:Float(도, 기본0), maxSpeed:Float(km/h, 기본0), duration:Long(ms, 기본0). 근거: `RideEntity.kt`
- DB 버전 3. `maxSpeed` 컬럼은 버전 3에서 추가됨. 현재 마이그레이션 정책은 **개발 단계 — 파괴적 마이그레이션**(`fallbackToDestructiveMigration`)으로, 명시적 마이그레이션 미정의 경로는 DB를 재생성한다(데이터 미보존). 🔲 TBD(출시 전): 데이터 보존이 필요하면 명시적 `Migration` 작성으로 전환 필요. 근거: `SpeedoDatabase.kt:12`, `DatabaseModule.kt`
- `TelemetryEntity`(`telemetry_logs`): id(PK,autoInc), rideId:Long, timestamp:Long, speed:Float, roll:Float, brakeEvent:BrakeEvent, brakeForce:Float, latitude:Double?, longitude:Double?. 근거: `TelemetryEntity.kt`
- `rideId`는 DB 외래키 제약 없이 컬럼으로만 존재. 주행 삭제 시 연관 telemetry_logs는 애플리케이션 코드에서 수동 삭제(F-15). 🔲 TBD(목표): FK 제약/인덱스 도입 여부.
- 도메인 모델: `LocationData`(lat,lng,speed,accuracy), `GravityData`(x,y,z), `AccelerometerData`(x,y,z,timestamp), `TpmsData`(앞/뒤 pressurePsi/temperature/batteryVoltage, timestamp). 근거: `SensorData.kt`
- `TelemetryEntity.roll`은 **부호 있는** 기울기(좌/우 방향 보존). `latitude/longitude`는 nullable이며, 목표 설계(F-13c)에서 센서 시간주기 행은 null, GPS 픽스 행만 실좌표를 가진다.

### 3.2a 텔레메트리 샘플링 정책 (확정)
- **GPS**: `PRIORITY_HIGH_ACCURACY`, 요청 주기 1000ms(최소 500ms) = 1~2Hz. **현행 1Hz 유지**(GNSS 실효 상한이 보통 1Hz, 충전 사용 전제로 배터리 절감 동기 없음, GPS 하향 시 코너 보간 오차 `s≈d²/(8R)`가 간격 제곱으로 증가하여 path·거리 정확도 악화). 근거: `LocationDataSource.kt:53-57`
- **중력 센서**: `SENSOR_DELAY_UI`(약 60ms, ~16Hz). 근거: `GravitySensor.kt:29`
- **목표 저장 정책(F-13b)**: 기울기·제동을 GPS 비종속 **시간주기 200ms(5Hz)** 로 저장(부호 있는 순간 기울기). 위치는 GPS 픽스 시점만 채우고 나머지는 보간(F-13c). 주기 100~500ms 범위 조절 가능, 기본 200ms. 저장량 5Hz×2시간 ≈ 36,000행/주행.

### 3.2b 경로 정밀도 — 검토했으나 보류한 대안 (Design History)
F-13c(시간주기 행의 지도 위치 결정)의 대안으로 아래를 검토했고, 현재는 **보류**한다. 채택안은 F-13c(선형 보간, 미관 옵션 Catmull-Rom).

- **대안 A — GPS 레이트 상향(best-effort 5Hz, 미지원 시 1Hz 폴백)**
  - 내용: `LocationRequest` 주기를 ~200ms로 요청해 실측 앵커 밀도를 높임. 기기 미지원 시 1Hz로 자연 폴백.
  - 장점: **실측 기반**이라 모델 위험 0, 코너 path·거리 정확도 향상. 충전 사용 전제로 배터리 제약 없음.
  - 보류 사유: 효과가 **기기별 GNSS 실제 출력률에 의존**(다수 기기 1Hz 상한)하여 보장 불가. 1Hz 환경에선 현(chord) 오차가 이미 ~1.2m(60km/h·R=30m) 수준이라 단순 보간 대비 한계 이득. → "여유되면 적용" 후보로 보류.
- **대안 B — 물리 기반 동역학 보간(lean→yaw dead reckoning + GPS 앵커 보정)**
  - 내용: 모터사이클 정상선회식 `tan θ = v²/(gR)` → `ω = g·tanθ/v`로 조밀 lean과 속도로부터 회전각속도를 추정, 헤딩·궤적을 적분하고 양끝을 GPS로 구속해 코너를 호(arc)로 복원.
  - 장점: 이론상 좌표·센서 빈도를 물리적으로 정합. GPS가 희박할수록 이득이 큼.
  - 보류 사유(객관적 결함): ① **정상상태 가정**이 턴인/트레일브레이킹/연속코너/**노면 뱅킹**에서 깨짐. ② 측정값이 진짜 lean이 아니라 **중력센서 roll**이라 선회 중 원심가속도로 오염됨(Rotation Vector 도입으로 완화되었으나 여전히 모델 단순화 한계 존재). ③ 적분 **드리프트** → 곡률 최소화 스무더 등 추가 설계 필요. ④ **자이로 활용 가능성**: `RotationVectorSensor` 도입으로 자이로 데이터 활용이 가능해졌으나, lean→yaw 모델 단독 의존보다는 직접적인 yaw rate 측정이 권장됨. ⑤ 산출물이 시각화(지도 path 위치)인데 풀 센서퓨전 비용이 과함. ⑥ 1Hz에선 체감 이득 작음.
  - 재추진 전제: 채택 시 **자이로(`TYPE_GYROSCOPE`/rotation-vector)를 통해 yaw를 직접 측정**하거나 융합할 것. → R&D/희박 GPS 시나리오 전용으로 보류.

### 3.3 외부 의존성 / 권한
- 권한(매니페스트 선언): ACCESS_FINE/COARSE_LOCATION, BODY_SENSORS, FOREGROUND_SERVICE(+_LOCATION), POST_NOTIFICATIONS, INTERNET, BLUETOOTH/_ADMIN/_SCAN(neverForLocation)/_CONNECT, BILLING, SYSTEM_ALERT_WINDOW. 근거: `AndroidManifest.xml:5-18`
  - 런타임 권한 요청 플로우(확정):
    - 위치(FINE/COARSE) + 알림(Android 13+): Splash 진입 시 요청. **거부 시 안내 후 앱 종료**(핵심 권한 게이트, §2.1·§4.6).
    - BLE(BLUETOOTH_SCAN/BLUETOOTH_CONNECT): **이번 버전에서는 TPMS 기능 비활성화로 인해 권한을 요청하지 않음.** (향후 활성화 시 설정 화면에서 TPMS 연결 시도 시점 요청으로 환원)
    - 다른 앱 위에 표시(`SYSTEM_ALERT_WINDOW`): **설정에서 오버레이 위젯 기능을 활성화하는 시점**에 요청. 시스템 설정 화면으로 이동하여 직접 허용 유도. 거부 시 기능을 끈다.
- 센서: FusedLocation(GPS), Gravity, Accelerometer, Gyroscope, Rotation Vector, Game Rotation Vector(F-03 측정 전략별 사용). 진단 CSV 로깅(F-03)은 **주행 기록(F-09) 중 자동으로** 전 센서 + GPS 를 동일 timestamp 로 앱 전용 외부 저장소(`getExternalFilesDir/lean_diag`)에 기록하고(주행 1건당 파일 1개), 설정의 **Export** 버튼으로 개발자에게 메일 전송(FileProvider, `ACTION_SEND_MULTIPLE`). 근거: `data/sensor/lean/LeanDiagnosticLogger.kt`, `TelemetryRepositoryImpl`(기록 연동)
- 통신: BLE 스캔(SCAN_MODE_LOW_LATENCY)으로 TPMS 패킷 수신. 근거: `TpmsDataSource.kt:79-82`
- 서드파티: Google AdMob(App ID 매니페스트 선언), Google Play Billing, Google Maps(`MAPS_API_KEY`). 근거: `AndroidManifest.xml:32-43`

### 3.4 Architecture & UDF Mapping
- **UI Layer**:
  - `UiState`: `LogState` (isLoading, title, date, duration, distance, maxLean, maxSpeed, routePoints, selectedPoint)
  - `UiEvent`: `selectPoint(TelemetryEntity?)`
- **Domain Layer**: `GetRideDetailUseCase`, `GetRideTelemetryUseCase`
- **Data Layer**: `RideDao`, `TelemetryDao`, `RideEntity`, `TelemetryEntity`
- **Required Resources**:
  - `Color`: `LeanGreen_Dark`(0xFF15803D), `LeanYellow_Dark`(0xFFB45309), `LeanOrange_Dark`(0xFF9A3412), `LeanRed_Dark`(0xFF7F1D1D)
  - `Drawable`: `ic_path_arrow` (경로 진행 방향 표시용 삼각형)

---

## 4. Exception Handling & Edge Cases

### 4.1 센서/GPS
- GPS 미수신(0,0 좌표): 속도 0 처리, 거리 누적 제외. 근거: `RideDistanceTracker.kt:48`, `TelemetryRepositoryImpl.kt:133`
- GPS 정지 노이즈: 속도 정확도 게이트 + 0.7 m/s 데드밴드로 0 처리. 근거: `SpeedResolver.kt:25-28`
- 저정확도 측위(>25m): 거리 누적에서 제외. 근거: `TelemetryRepositoryImpl.kt:223`
- 기울기 센서 무효(0,0,0): roll 0° 반환(영점만큼 음수 표시되는 오류 방지). 근거: `SensorData.kt:34-36`
- **기록 데이터 불일치 이슈**: 대시보드 실시간 표시는 정상이나, 저장된 로그의 뱅킹각이 실제보다 현저히 낮게 기록되는 이슈가 보고됨. 해당 로직은 `TelemetryRepositoryImpl.kt`의 데이터 집계 및 저장 주기와 관련이 있는 것으로 파악됨.
- **기울기 측정 방식**: 코너 최대 기울기에서 실측(영상) 대비 약 10~15% 과소 측정되는 문제는 **고정 배율 보정으로 해결 불가**(정상선회 센서융합 오염은 비선형). 이에 단일 보정 비율 대신 **선택형 측정 전략(F-03)** 으로 대응하고, 실주행 비교로 최적 방식을 채택한다. 장착 오정렬(일정 비율형)은 별개 문제로 보아 현행 스칼라 offset(F-04) 유지. (F-03, §3.2b 참고)
- 기록 시 기울기는 구간 내 절대값 최대 샘플의 **부호를 보존**해 저장(좌/우 방향 유지). 세션 요약 maxLean은 절대값. 근거: `TelemetryRepositoryImpl.kt:116-127`
- 영점 보정 중 유효 샘플 미수신: 2,000ms 타임아웃 후 기존 offset 유지. 근거: `LeanCalibrationRepositoryImpl.kt:33-42`
- **위치 음영 구간(터널 등) 속도 미수신 대응**: GPS 신호 유실로 인해 실시간 속도를 측정할 수 없는 음영 구간(예: 터널) 진입 시, 속도가 단순히 0으로 처리되지 않도록 진입 직전 정보와 진출 직후 정보(시각, 수신 좌표 등)를 기반으로 해당 유실 구간의 속도를 보간하여 처리한다. 이 보간 데이터는 주행 상세 로그 지도 및 텔레메트리 기록 상에서 유실 없이 표시/기록될 수 있도록 기획적 방향성을 제공하며, 상세 보간 로직은 개발 단에서 기존 코드를 분석하여 성능과 정확도의 균형을 맞추어 구현한다.

### 4.2 TPMS / BLE
- 블루투스 비활성/스캐너 없음: 스캔 시작하지 않음(예외 없이 무시). 근거: `TpmsDataSource.kt:59`
- 센서 ID 미설정: 스캔 시작하지 않음. 근거: `TpmsDataSource.kt:52`
- payload 파싱 예외: catch 후 로그만 남기고 무시. 근거: `TpmsDataSource.kt:128-130`
- 압력 데이터 없음(current≤0): 경고색 중립 표시. 근거: `DashBoardViewModel.kt:53`
- **⚠️ 이번 버전 비활성화 (숨김)**: 실제 센서 데이터 검증 미흡 및 오동작 노출을 막기 위해, 이번 버전에서는 대시보드와 설정 화면 등 앱 전반의 UI에서 TPMS 노출을 전체 비활성화(숨김) 처리한다. 향후 실제 센서 프로토콜 사양과 정합성을 검증하여 기능을 정상화할 예정이다.
- **TPMS 데이터 로깅 (분석용) 비활성화**: TPMS 기능 비활성화와 함께 로깅 옵션 제공 및 파일 저장 기능도 이번 버전의 앱 동작 범위에서 제외한다.

### 4.3 공백/빈 데이터
- 주행 목록 0건: 현재 전용 빈 상태(Empty State) UI 없음. 헤더 + 총거리 카드(0.0) + 빈 리스트만 렌더링. 🔲 TBD(목표): 빈 상태 안내 문구/일러스트 정의 여부. 근거: `RecordsScreen.kt:115-128`
- 주행 상세 텔레메트리 0건: 경로 점 0개 → Polyline 미표시(점 2개 미만이면 선 없음), 카메라는 마지막 위치 또는 기본값. SESSION SUMMARY는 표시되나 TOP SPD=0. 근거: `LogScreen.kt:269-300,318`
- 위치 보간(F-13c, 목표): 위치 null인 시간주기 행은 앞뒤 GPS 앵커의 타임스탬프 비례로 보간한다. 한쪽 앵커만 존재(주행 시작 전/종료 후 구간)하면 가장 가까운 앵커로 클램프한다. GPS 앵커가 0개면 path를 그리지 않는다.
- 잘못된 rideId(-1) 또는 미존재 주행: Log 화면에 각각 "Error: Invalid Ride ID" / "Record not found" 표시(영문 하드코딩). 🔲 TBD(목표): 다국어/UX 정의. 근거: `LogViewModel.kt:47,70`

### 4.4 유효성 / 입력 검증
- 🔲 TBD: TPMS 센서 ID 입력 형식 검증(MAC 형식/길이/허용 문자) 규칙.
- 주행 제목 수정(F-14): trim 후 빈 문자열이면 저장 거부, 저장 버튼 비활성화. 최대 길이 제한 없음. 🔲 TBD(목표): 최대 길이 도입 여부.
- 적정공기압 입력 단위는 설정의 압력 단위(PSI/BAR)를 따르고 내부는 PSI로 저장. 기본값 앞 36/뒤 40 PSI. 입력 허용 범위 **10~60 PSI(경계 포함)**, 범위 밖 입력은 거부.

### 4.5 결제 / 네트워크
- Billing 서비스 연결 끊김: 다음 요청 시 재연결 시도(현재 자동 재시도 미구현). 근거: `BillingRepository.kt:54-57`
- Billing 응답 비정상(OK 아님): 구매/상품 갱신 미수행. 근거: `BillingRepository.kt:48,67,86`
- 🔲 TBD: 광고 로드 실패 시 동작, 오프라인 시 동작 정의.

### 4.6 권한 거부
- **현재 동작**: 위치/알림 권한을 Splash에서 일괄 요청하나, 허용 여부와 무관하게 메인으로 진행(차단/재안내 없음). 근거: `SplashScreen.kt:62-64,79-83`
- **목표(To-Be) 확정**:
  - 위치/알림 거부 → "이 권한이 있어야 앱을 사용할 수 있다"는 안내 표시 후 앱 종료. 메인 진입 차단.
  - BLE 권한 거부 → 이번 버전에서는 TPMS 기능 비활성화로 인해 발생하지 않음. (설정 화면에서 진입 차단)

### 4.7 주행 종료 감지 예외
- **장기 정차 시 기록 유지**: 사용자가 알림/팝업에서 [계속]을 선택하거나 응답하지 않을 경우, 데이터 유실 방지를 위해 주행 기록은 자동으로 종료하지 않고 계속 유지한다.
- **감지 타이머 리셋**: 속도가 0.7km/h(F-01 기준 0)를 초과하는 즉시 감지 타이머를 초기화한다. [계속] 선택 시에도 타이머를 초기화하며, 이후 다시 T분간 정차 시 재발행한다.
- **포그라운드/백그라운드 병행**: 앱이 화면에 떠 있을 때는 다이얼로그 팝업을 우선하며, 백그라운드 상태이거나 화면이 꺼진 경우에는 알림을 통해 주행 종료 여부를 확인한다.

### 4.8 플로팅 오버레이 예외
- **권한 미허용**: `SYSTEM_ALERT_WINDOW` 권한이 없을 경우, 오버레이 기능을 활성화하려 할 때마다 설정 화면 이동 안내 팝업을 표시한다.
- **메모리 부족(LMK)**: 시스템에 의해 오버레이 서비스가 강제 종료될 경우, 기록 중인 포그라운드 서비스가 살아있다면 재시작 시 오버레이를 복구한다.
- **화면 캡처/녹화**: 오버레이는 `Secure Flag`를 사용하지 않으므로 화면 캡처 시 포함될 수 있음을 사용자에게 인지시키지 않으나, UI가 너무 크지 않게 설계한다.
- **멀티 윈도우 모드**: 분할 화면 모드에서는 오버레이를 표시하지 않거나, 사용자가 수동으로 닫을 수 있는 'X' 버튼을 항상 상단에 작게 배치한다.

---

## 5. 미해결 항목 요약 (Open Items — 임의 판단 보류)
다음은 코드만으로 확정할 수 없어 사용자 확답이 필요한 항목이다. 확정 전까지 위 본문에 🔲 TBD 로 유지한다.
1. ~~프리미엄 잠금 범위~~ — ✅ **확정**: 광고 제거 전용, 기능 게이팅 없음 (§1.3)
2. ~~Splash 노출 시간~~ — ✅ **해결**: 권한 결정 후 2,000ms (§2.1)
3. ~~적정공기압 사용자 설정~~ — ✅ **확정**: 단위=설정 따름, 저장=PSI, 기본 36/40, **입력 범위 10~60 PSI** (F-08a, §4.4)
4. **TPMS 실 센서 프로토콜 정상화** — 🔧 **향후 과제**: 현재 전혀 동작하지 않으며, 실 센서 프로토콜(바이트 매핑) 실측 및 연결 끊김 처리 로직 전체 재작성 필요 (F-07, §4.2)
5. ~~Records/Log 화면 사양~~ — ✅ **해결**: 정렬·필드·지도/색상·구간 텔레메트리 명세 완료 (F-12, F-13, F-13a). Records "TOP SPD" 하드코딩 불일치도 ✅ 수정 완료(F-12a, 실제 `maxSpeed` 표시. DB는 버전 3, 개발 단계 파괴적 마이그레이션)
6. ~~제목/삭제 유효성·cascade~~ — ✅ **해결**: trim+non-blank, 수동 cascade 명세 완료 (F-14, F-15, §3.2)
7. ~~런타임 권한~~ — ✅ **확정**: 위치/알림 거부 시 안내 후 종료(핵심 게이트), BLE는 설정 TPMS 연결 시점 요청 (§3.3, §4.6). ⚠️ 알림 시작-차단은 재검토 권고
8. ~~빈 상태~~ — ✅ **해결**: 현재 전용 Empty State 없음을 사실 기재. 목표 도입 여부만 ❓ (§4.3)
9. ~~LogScreen 기울기 방향(L/R) 버그~~ — ✅ **수정 완료**: abs 저장 제거, 부호 보존 (F-13a, §4.1)
10. ~~기울기 기록 누락 및 데이터 불일치~~ — ✅ **구현됨**: 시간주기 200ms 분리 저장(F-13b, 커밋 363e6d2)·위치 보간(F-13c, 커밋 72d13b3) 완료. ※ 저장 roll 과소표기 보정(F-03/§4.1)은 별도 미해결로 잔존.
11. ~~주행 로그 제동 미기록~~ — ✅ **구현됨**: 시간주기 행에 제동 이벤트/세기 동적 저장 완료 (F-05, F-13b)

> 범례: ✅ 해결(코드 분석/수정 완료) 또는 설계 확정 · 🔧 구현 대기(설계 확정됨) · ❓ 제품 결정 필요 · ⚠️ 검토 권고/유의
>
> ✅ 구현 완료: F-13b(기울기/제동 시간주기 200ms 분리 저장, 커밋 363e6d2), F-13c(경로 위치 보간 — 선형/Strategy, 커밋 72d13b3). 스키마 무변경. 보류한 대안(GPS 레이트 상향, 물리 기반 동역학 보간)은 §3.2b에 설계 이력으로 기록.
>
> 코드-의도 불일치 추적: (1) Records "TOP SPD" 하드코딩 → ✅ `maxSpeed` 표시로 수정(F-12a). (2) BLE 권한 런타임 미요청 → ✅ 설정 TPMS 연결 시점 요청으로 목표 정의(§3.3·§4.6). (3) LogScreen 기울기 방향 abs 버그 → ✅ 부호 보존으로 수정(F-13a).
n 기울기 방향 abs 버그 → ✅ 부호 보존으로 수정(F-13a).
