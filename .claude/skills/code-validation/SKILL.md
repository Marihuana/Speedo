---
name: code-validation
description: 작성된 코드가 빌드, 린트, 아키텍처 규칙 및 PRD 요구사항을 완벽히 충족하는지 교차 검증하고, 에러를 스스로 분석하여 수정(Reflection)하는 가이드라인입니다.
---

# Code Validation Skill (독립 교차 검증 및 자체 수정)

이 스킬은 구현(Implementation) 단계가 끝난 후, 코드가 실제 동작 가능하고 부작용이 없는지 **독립적으로 교차 검증**하기 위해 사용됩니다. 코드를 작성하는 모드에서 벗어나, 깐깐한 QA 엔지니어이자 코드 리뷰어의 시각으로 접근하세요.

## 1. 🏗️ 빌드 및 정적 분석 (Build & Lint)
구현이 끝났다고 판단되면, 즉시 터미널(`Bash`)에서 다음 명령어를 실행하여 1차 검증을 시작합니다.
- 안드로이드 빌드 검증: `./gradlew assembleDebug --quiet` (또는 `:app:compileDebugKotlin`)
- (필요시) 정적 분석: `./gradlew lint --quiet`
- (테스트 영역이 있으면) `./gradlew test`

빌드 도구 사용이 불가한 환경이면 그 사실과 수동 검토 결과를 명시합니다.

## 2. 🔍 에러 분석 (Error Analysis)
에러가 발생한 경우, 사용자에게 묻지 않고 스스로 로그를 정독하여 근본 원인을 파악합니다. 주로 다음 항목들을 의심하세요:
- [ ] **Unresolved reference**: 리소스(`R.string...`, `R.drawable...`) 네이밍 오타 또는 `import` 누락.
- [ ] **Hilt/Dagger Error**: `@Binds`, `@Provides`, `@Inject` 누락 또는 모듈 선언 불일치.
- [ ] **Type Mismatch**: UDF 구조 내 `UiState`와 Compose 컴포넌트의 매개변수 타입 불일치.

## 3. 🛠️ 자체 수정 및 리플렉션 (Self-Correction Loop)
- 에러의 원인을 파악하면 타겟 파일을 `Edit` 도구로 수정합니다.
- **반드시 에러가 0이 될 때까지 [빌드 -> 분석 -> 수정] 루프를 스스로 반복(Reflection)합니다.**
- 이 과정에서 근본적인 아키텍처 결함이 발견되면 설계를 재검토하고 수정을 진행하세요.

## 4. ✅ 의미론적 검증 (Semantic Verification)
컴파일 에러가 없더라도, 작성된 코드가 PRD 문서를 완벽히 충족하는지 2차 검증을 수행합니다.
- PRD의 **Expected Behavior (To-Be)** 와 코드가 정확히 일치하는가? (임계값·변환식·단위·부등호 경계 포함/제외까지)
- PRD의 **Testing Strategy** (단위 테스트 시나리오, 화면 전환 등)를 통과할 수 있는 논리인가?
- 코드 내부에 문자열이 하드코딩되지 않고 `stringResource`가 사용되었는가?

모든 검증이 끝나면 "교차 검증 및 빌드 테스트를 성공적으로 마쳤습니다."라고 보고하며 임무를 종료합니다.
