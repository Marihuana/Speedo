---
name: prd-generation
description: docs/PRD_TEMPLATE.md를 기반으로 프로젝트 코드를 분석하여 PRD 초안을 생성하거나 업데이트합니다. feat/fix 모드를 지원합니다.
---

# PRD Generation Skill

이 스킬은 프로젝트의 코드베이스를 정밀 분석하여 `docs/PRD_TEMPLATE.md` 형식에 맞는 고품질 PRD 초안을 작성합니다.

## 1. 🔍 사전 분석 (Technical Context Extraction)
- **인터페이스 요약:** 소스 코드 전체를 읽기 전, `ViewModel`의 필드/메서드 서명, `Repository` 인터페이스, `Entity` 클래스 구조만 먼저 추출하여 컨텍스트 요약을 생성합니다. (토큰 효율화)
- **기술적 영향도 분석(Impact Analysis):** 신규 기능/버그 수정이 시스템 전반에 미치는 영향을 분석합니다.
  - 수정/추가될 `Hilt Module`, `Database Migration` 필요 여부, `UseCase` 인터페이스 변경점을 리스트업합니다.
  - 관련 컴포넌트 간의 의존성 변화를 파악합니다.
- **로직 정밀 추출:** 요약된 구조를 바탕으로 실제 로직 파악이 필요한 핵심 메서드만 선별적으로 `read_file`하여 비즈니스 규칙을 추출합니다.

## 2. ✍️ 템플릿 매핑 (Technical Spec Mapping)
- `docs/PRD_TEMPLATE.md`의 구조를 100% 준수하되, 기술적 상세를 강화합니다.
- **데이터 계약(Data Contract) 명세:** 3.2 섹션을 스키마 정의 수준으로 상세화합니다.
  - 데이터 타입, Nullability, 기본값, 인덱스 전략, 마이그레이션 정책을 명시합니다.
- **스레딩 정책:** 데이터 흐름이 발생하는 각 지점의 `Coroutine Dispatcher` 정책을 명시합니다.
- **근거 명세:** 모든 기능 뒤에 `(근거: 파일명:심볼명)`을 명시하여 추적 가능성을 확보합니다.

## 3. 🛠 기술적 깊이 (Technical Depth)
- **UI/UX:** Figma 노드 ID가 없는 경우 사용자에게 질문하거나 `🔲 TBD` 처리합니다.
- **Architecture:** `UiState` 필드와 `UiEvent` 흐름을 코드를 바탕으로 구체적으로 명세합니다.
- **Resources:** 필요한 `strings.xml` 키와 리소스 파일명을 예측하여 명시합니다.

## 4. 🚀 출력 및 승인 (Output & Approval)
- **사전 보고:** `PRD.md` 파일을 작성하거나 수정하기 전, 반드시 변경될 핵심 내용(요구사항 요약, 수정된 사양, 기술적 근거 등)을 사용자에게 먼저 보고합니다.
- **승인 후 반영:** 사용자의 명시적인 승인이 있은 후에만 `write_file` 또는 `replace` 도구를 사용하여 파일에 반영합니다.
- 최종 결과물은 `PRD.md` 파일로 산출하며, 기존 문서가 있는 경우 `replace`를 통해 변경된 부분만 Surgical Update 합니다.
