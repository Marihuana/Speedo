# Speedo Project Instructions

This file contains project-specific conventions, architecture rules, and workflows for the Speedo Android project.

## PRD & Documentation
- **Standard Template:** PRD(Product Requirements Document)를 작성하거나 업데이트할 때는 반드시 `docs/PRD_TEMPLATE.md` 파일을 템플릿으로 사용해야 합니다.
- **Evidence-Based:** 모든 기능 명세에는 가능한 경우 코드상의 근거(파일 경로 및 라인 번호)를 명시해야 합니다.
- **TBD Management:** 확정되지 않은 항목은 `🔲 TBD` 표시를 유지하고 임의로 판단하지 않습니다.
- **Routing Rules:** 사용자의 요청이 `feat:` 접두사로 시작하는 경우 `speedo-prd-expert`를, `fix:` 접두사로 시작하는 경우 `speedo-issue-specialist` 서브 에이전트를 호출하여 `PRD.md`를 관리합니다. 특히 다음 두 가지 철칙을 준수해야 합니다:
    1. **PRD 전담:** 에이전트(메인 및 서브 포함)는 어떠한 경우에도 직접적인 코드 수정이나 개발 작업을 수행하지 않으며, 오직 PRD 작성 및 기술 명세 정의에만 집중합니다.
    2. **보고 후 승인:** 모든 문서 작성 작업(`write_file`, `replace` 등)을 수행하기 전에, 반드시 작성할 핵심 내용을 사용자에게 먼저 보고하고 명시적인 승인을 받은 경우에만 파일에 반영합니다.

## Android Skills & Capabilities

### Jetpack Compose & UI
- **Description:** UI development using Jetpack Compose and Material 3.
- **Conventions:**
    - Use Material 3 components exclusively.
    - Organize UI code in `ui/theme/` and feature-specific packages.
    - Prefer `StateFlow` and `SharedFlow` for state management in `ViewModel`.
    - Maintain strict separation between UI and business logic.

## Git Commit Convention (git-convention skill)
- **Gitmoji:** 모든 커밋 메시지는 적절한 Gitmoji로 시작합니다.
- **Title:** `<gitmoji> <type>: <description>` 형식으로 작성하며, **명령형**으로 간결하게 표현합니다 (소문자 시작, 마침표 생략).
- **Body:** 변경 이유와 상세 내용을 가급적 자세히 기술합니다.
- **Atomic Commits:** 변경 사항을 한꺼번에 커밋하지 않고, **의도별로 쪼개어** 관리하여 추적성을 높입니다.
