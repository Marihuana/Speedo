---
name: android-development
description: 안드로이드 개발을 위한 가이드입니다. Jetpack Compose로 UI를 생성하거나, 앱 계층(MVI/UDF)을 설계하거나, 데이터/도메인 로직을 관리하거나, 의존성 주입을 위해 Hilt를 구성할 때 사용하세요.
---

# Android 개발 스킬 (Android Development Skill)

이 스킬은 구글의 최신 안드로이드 아키텍처 가이드라인(Modern Android Architecture)을 기반으로 코드를 작성하고 리팩토링하는 데 도움을 줍니다.

## 핵심 원칙 (Core Principles)

1.  **계층형 아키텍처 (Layered Architecture)**: UI, Domain, Data 계층으로 명확히 분리하여 관심사를 분산합니다.
2.  **단방향 데이터 흐름 (UDF)**: 상태(State)는 아래로 흐르고, 이벤트(Event)는 위로 흐르는 구조를 유지합니다.
3.  **Hilt를 통한 의존성 주입**: 모든 컴포넌트 간의 결합도를 낮추기 위해 Hilt를 사용합니다.
4.  **Jetpack Compose & Material 3**: 선언형 UI 프레임워크와 최신 디자인 시스템을 활용합니다.

## 워크플로우 (Workflows)

안드로이드 관련 작업을 수행할 때, 다음의 참조 문서를 상황에 맞춰 활용하세요:

-   **UI 및 화면 개발**: Jetpack Compose 규칙, 상태 관리 및 프리뷰 작성법은 `references/ui_layer.md`를 참고하세요.
-   **비즈니스 로직 및 Use Case**: 복잡한 로직의 캡슐화 및 재사용은 `references/domain_layer.md`를 참고하세요.
-   **데이터 관리 및 Repository**: 데이터 소스(Room, Retrofit) 조율 및 오프라인 우선 전략은 `references/data_layer.md`를 참고하세요.
-   **의존성 주입 (DI)**: Hilt 설정 및 모듈 구성은 `references/di_hilt.md`를 참고하세요.
-   **코틀린 스타일 및 Coroutine**: 코드 스타일 및 비동기 작업 처리 규칙은 `references/style.md`를 참고하세요.

## 시작하기

새로운 기능을 추가하거나 기존 코드를 리팩토링할 때, 먼저 해당 기능이 속한 계층을 파악하고 관련된 참조 문서를 읽어 아키텍처 표준을 준수하세요.
