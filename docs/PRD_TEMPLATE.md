# PRD: [Project Name]

> 작성: [Agent Name] | 최종 수정: YYYY-MM-DD
> 상태: [Draft / Confirmed]
> 패키지: `[com.example.app]` | 아키텍처: [e.g. Clean Architecture + MVI]
> 기준: [e.g. To-Be (목표 사양) - 현재 구현 사실 + 완성 목표 명시]

---

## 1. Project Overview

### 1.1 목적 (Purpose)
- [앱이 해결하고자 하는 핵심 문제와 가치, 또는 **수정하고자 하는 주요 버그/개선점** 기술]

### 1.2 타겟 유저 (Target User)
- [사용자 정의, 주요 사용 환경, 사용 맥락]

### 1.3 수익 모델 (Revenue Model)
- [무료/유료, 인앱 결제, 광고 포함 여부 등]

---

## 2. Core User Flows & Features (Improvements / Bug Fixes)

### 2.1 Screen & Navigation Flow
- [앱 진입(Splash)부터 주요 메인 탭, 상세 화면까지의 화면 구조 및 이동(Route) 조건 명시]

### 2.2 Features & Fixes
*(주의: 신규 기능(F-xx), 버그 수정(B-xx), 개선 사항(I-xx)을 모두 포함할 수 있습니다. 버그 수정 시 Input/Output에 As-Is(현재 문제)와 Expected Behavior에 To-Be(수정 목표)를 명확히 대비하여 작성하세요.)*

| # | Feature / Bug Fix | Input (As-Is) | Output | Expected Behavior (To-Be) | UI/UX (Figma Link) | 근거 (코드/사용자확답) |
|---|-------------------|---------------|--------|--------------------------|--------------------|----------------------|
| [F/B/I-01] | [기능명/버그명] | [입력값/발생조건] | [출력/결과] | [검증 가능한 올바른 동작 설명] | [Figma Node ID 등] | [파일:라인 또는 확답] |

---

## 3. Data & Technical Requirements

### 3.1 클라이언트 상태 범위 (Client State Scope)
- **화면(휘발)**: [UI 전용 상태]
- **세션(메모리)**: [프로세스 유지 기간 동안만 유효한 상태]
- **영구(Persistence)**: [DataStore, Room, SharedPreferences 등 저장 데이터]

### 3.2 필수 데이터 구조 (Data Structures)
- [핵심 Entity 또는 데이터 모델의 필드 및 제약 조건]

### 3.3 외부 의존성 / 권한
- [필요 권한, 센서, 네트워크 API, 서드파티 SDK 등]

### 3.4 Architecture & UDF Mapping
- **UI Layer**:
  - `UiState`: [상태 클래스명 및 주요 필드/기본값 명시]
  - `UiEvent`: [사용자 인터랙션 및 ViewModel 트리거 이벤트]
- **Domain Layer**: [추가/수정될 UseCase 및 Repository Interface]
- **Data Layer**: [수정될 Entity, API, DataSource]
- **DI (Dependency Injection)**: [Hilt 모듈에 추가되어야 할 `@Binds` 또는 `@Provides` 바인딩 명시]
- **Required Resources**: [기존 재사용 또는 신규 추가가 필요한 `R.string`, `R.drawable`, `Color` 목록 명시. **에이전트의 임의 식별자 네이밍 및 UI 내 문자열 하드코딩 절대 금지. 반드시 strings.xml 사용**]

---

## 4. Exception Handling & Edge Cases

*(주의: 모든 예외 상황에 대해 사용자에게 어떻게 안내할지(Toast, Snackbar, Dialog, 전체 에러 화면 등) 구체적인 UI 피드백 방식을 반드시 명시할 것)*

### 4.1 센서 및 외부 입력 예외
- [센서 데이터 무효, 미수신 시 동작 및 UI 피드백]

### 4.2 데이터 공백 및 초기 상태
- [빈 리스트, 초기 로딩 화면 등 Empty State 명시]

### 4.3 유효성 및 입력 검증
- [입력 제한, 범위 초과 시 동작 및 UI 피드백]

### 4.4 권한 및 환경 예외
- [권한 거부 또는 필수 하드웨어 부재 시 동작 및 UI 피드백]

### 4.5 생명주기 및 시스템 이벤트 (Lifecycle & System Events)
- [백그라운드 전환, 홈 버튼, OS 강제 종료(System Kill) 시 데이터 보존 및 복구 정책]

---

## 5. Testing & Validation Strategy

### 5.1 Unit Test Scenarios (단위 테스트)
- [ ] **[테스트명]**: Given [초기 상태/조건] -> When [어떤 동작 발생] -> Then [기대하는 결과/상태 변화]

### 5.2 Manual / UI Test Scenarios (수동 및 통합 검증)
- [ ] **[시나리오명]**: [구체적인 조작 순서 및 화면에 나타나야 할 최종 피드백 명시]

---

## 6. 미해결 항목 요약 (Open Items)
- 🔲 TBD: [사용자 확인 또는 추가 분석이 필요한 항목]
