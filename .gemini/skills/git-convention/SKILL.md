---
name: git-convention
description: Gitmoji와 구조화된 메시지를 사용하여 의도별로 분리된 깨끗한 커밋 이력을 관리합니다.
---

# Git Convention Skill

이 스킬은 프로젝트의 커밋 이력을 명확하고 추적 가능하게 유지하기 위한 규칙을 정의합니다. 모든 커밋 작업 시 이 규칙을 엄격히 준수합니다.

## 1. 🎨 Gitmoji 사용 (Use Gitmoji)
- 모든 커밋 메시지의 시작은 성격에 맞는 Gitmoji로 시작합니다.
- 예시:
    - ✨ `:sparkles:` (새로운 기능)
    - 🐛 `:bug:` (버그 수정)
    - 📝 `:memo:` (문서 수정)
    - 🎨 `:art:` (코드 스타일/포맷팅)
    - ♻️ `:recycle:` (리팩토링)
    - ✅ `:white_check_mark:` (테스트 추가/수정)
    - 📦 `:package:` (빌드 시스템/의존성 변경)
    - 👷 `:construction_worker:` (CI 설정)

## 2. 📝 커밋 메시지 구조 (Message Structure)
- **타이틀 (Title):** 
    - `<gitmoji> <type>: <description>` 형식으로 작성합니다.
    - **명령형**을 사용하며 간결하게 작성합니다 (예: "Fix bug" (O), "Fixed bug" (X), "Fixes bug" (X)).
    - 첫 글자는 소문자로 시작하며, 마침표(.)를 찍지 않습니다.
- **내용 (Body):**
    - 가급적 **자세하게** 작성합니다.
    - 변경 이유와 "무엇을", "어떻게" 변경했는지 설명합니다.

## 3. 🔪 커밋 쪼개기 (Atomic Commits)
- 변경 사항을 한꺼번에 커밋하지 않습니다.
- **의도별(Intent-based)**로 커밋을 쪼개어 관리합니다.
- 각 커밋은 하나의 논리적인 변화만 담고 있어야 하며, 이는 문제 발생 시 원인 추적(Git Bisect 등)을 용이하게 합니다.

## 4. 🚀 커밋 워크플로우 (Workflow)
1. 변경 사항 분석 및 논리적 단위 분리.
2. 단위별로 `git add` 수행 (필요시 `git add -p` 활용).
3. 위 컨벤션에 맞춰 커밋 메시지 작성 및 커밋.
