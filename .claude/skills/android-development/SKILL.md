---
name: android-development
description: 안드로이드 개발을 위한 가이드입니다. Jetpack Compose로 UI를 생성하거나, 앱 계층(UDF)을 설계하거나, 데이터/도메인 로직을 관리하거나, Hilt로 의존성 주입을 구성하거나, Foreground Service를 작성하거나, Navigation·런타임 권한·테스트를 다룰 때 사용하세요.
---

# Android 개발 스킬 (Android Development Skill)

이 스킬은 구글의 최신 안드로이드 아키텍처 가이드라인(Modern Android Development, MAD)을 기반으로 **Speedo** 프로젝트의 코드를 작성하고 리팩토링하는 데 도움을 줍니다.

> **단일 진실 공급원(Source of Truth)**: 이 스킬의 표준은 **Google MAD 패턴·구조·기술스택 + Clean Code + SOLID 원칙**입니다. 이 스킬은 기작성된 코드의 거버넌스가 아닙니다. 문서의 표준과 기존 코드가 충돌하면 **표준이 우선**하며, 기존 코드는 이 표준에 맞춰 정리될 대상으로 봅니다.

## 핵심 원칙 (Core Principles)

1.  **계층형 아키텍처 (Layered Architecture)**: UI, Domain, Data 계층으로 명확히 분리하여 관심사를 분산합니다.
2.  **단방향 데이터 흐름 (UDF)**: 상태(State)는 아래로 흐르고, 이벤트(Event)는 위로 흐르는 구조를 유지합니다. (`StateFlow`=상태 / `SharedFlow`=일회성 이벤트)
3.  **Hilt를 통한 의존성 주입**: 모든 컴포넌트 간의 결합도를 낮추기 위해 Hilt를 사용합니다.
4.  **Jetpack Compose & Material 3**: 선언형 UI 프레임워크와 최신 디자인 시스템을 활용합니다.
5.  **Clean Code & SOLID**: 의미 있는 이름, 작은 함수, 단일 책임(SRP), 인터페이스 의존(DIP)을 준수합니다. 추상화(`domain/repository` 인터페이스)에 의존하고 구현(`...Impl`)에 의존하지 않습니다.

## 프로젝트 구조 (Project Structure)

- 패키지 루트: `kr.yooreka.speedo`
- 계층별 디렉터리:
    - `ui/` — Compose 화면 + `[Feature]ViewModel` + `[Feature]State`. 테마는 `ui/theme/`, 공용 유틸성 컴포저블은 `ui/common/`.
      - **컴포넌트 배치 기준**: 여러 화면에서 쓰는 전역 재사용 컴포넌트는 `ui/components/`, 특정 화면 전용은 `ui/<feature>/components/`.
    - `domain/` — `domain/model`(순수 모델), `domain/repository`(Repository 인터페이스), `domain/usecase`(UseCase).
    - `data/` — `data/local`(Room `dao`/`entity`, DataStore `preferences`), `data/sensor`(`datasource` + `repository` 구현), `data/billing`.
    - `di/` — Hilt 모듈 (`RepositoryModule`, `DatabaseModule`).
    - `service/` — Foreground Service (`RecordingService`).
    - `utils/` — 프레임워크성 공용 헬퍼.

> Repository **인터페이스는 `domain/repository`**, **구현체(`...Impl`)는 `data/`** 에 둡니다(DIP).

## 기술 스택 (Tech Stack)

- Kotlin · AGP · KSP (Room/Hilt 컴파일러)
- Jetpack Compose (BOM) + Material 3
- Hilt · Navigation Compose · Room · DataStore Preferences
- Play Services Location/Maps · Maps Compose

> **버전은 이 문서에 적지 않습니다.** 정확한 버전의 단일 진실 공급원은 `gradle/libs.versions.toml` 버전 카탈로그이며, 의존성은 항상 카탈로그를 통해 추가/관리합니다.

## 워크플로우 (Workflows)

안드로이드 관련 작업을 수행할 때, 다음의 참조 문서를 상황에 맞춰 활용하세요:

-   **UI 및 화면 개발**: Jetpack Compose 규칙, 상태 관리, 일회성 이벤트(UiEvent) 처리, 프리뷰 작성법은 `references/ui_layer.md`를 참고하세요.
-   **화면 이동 (Navigation)**: Single-Activity / NavHost 구성, type-safe 라우트, 화면-네비게이션 분리는 `references/navigation.md`를 참고하세요.
-   **비즈니스 로직 및 Use Case**: 복잡한 로직의 캡슐화 및 재사용은 `references/domain_layer.md`를 참고하세요.
-   **데이터 관리 및 Repository**: 데이터 소스(Room, Retrofit) 조율, 오프라인 우선 전략, Flow vs suspend 규칙은 `references/data_layer.md`를 참고하세요.
-   **의존성 주입 (DI)**: Hilt 설정, 모듈 구성, 컴포넌트 범위(Scope) 선택은 `references/di_hilt.md`를 참고하세요.
-   **백그라운드 및 Service**: Foreground Service 작성, 알림 채널, Android 14 `foregroundServiceType`은 `references/service_layer.md`를 참고하세요.
-   **런타임 권한**: 위치/알림 권한 요청 및 거부 대응 패턴은 `references/permissions.md`를 참고하세요.
-   **테스트 작성**: 단위/계측 테스트, Fake 주입, 코루틴·Flow 테스트는 `references/testing.md`를 참고하세요.
-   **코틀린 스타일 및 Coroutine**: 코드 스타일 및 비동기 작업 처리 규칙은 `references/style.md`를 참고하세요.

## 빌드 & 테스트 (Build & Test)

```bash
./gradlew assembleDebug          # 디버그 APK 빌드
./gradlew installDebug           # 연결된 기기/에뮬레이터에 설치
./gradlew test                   # JVM 단위 테스트 (app/src/test)
./gradlew connectedAndroidTest   # 계측 테스트 (app/src/androidTest, 기기 필요)
./gradlew lint                   # 정적 분석
```

## 시작하기

새로운 기능을 추가하거나 기존 코드를 리팩토링할 때, 먼저 해당 기능이 속한 계층을 파악하고 관련된 참조 문서를 읽어 아키텍처 표준을 준수하세요.
