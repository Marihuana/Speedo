# UI Layer (Presentation Layer) 가이드

이 문서는 Jetpack Compose와 ViewModel을 활용한 UI 레이어 개발 원칙을 설명합니다.

## Jetpack Compose 개발 규칙

### 1. 명명 규칙 (Naming Conventions)
- **UI 컴포저블**: 명사(PascalCase)를 사용합니다. (예: `TPMSCard`, `DashBoardHeader`)
- **값 반환 함수**: 일반적인 코틀린 함수처럼 camelCase를 사용하며, 반환하는 값을 설명합니다. (예: `defaultCardColors()`)
- **상태 팩토리**: `remember` 블록을 내부적으로 사용하고 객체를 반환하는 경우 `remember` 접두사를 붙입니다. (예: `rememberScaffoldState()`)

### 2. 컴포넌트 구조
- **Modifier 파라미터**: 모든 UI 컴포저블은 `modifier: Modifier = Modifier` 파라미터를 가져야 합니다.
  - 이 파라미터는 **첫 번째 선택적 파라미터**여야 합니다.
  - 컴포저블 내부의 최상위 UI 노드에 이 `modifier`를 전달해야 합니다.
- **Emit XOR Return**: 컴포저블 함수는 UI를 방출하거나 값을 반환해야 하며, 두 가지를 동시에 수행해서는 안 됩니다.
- **Trailing Lambdas**: `content` 람다 블록은 항상 마지막 파라미터로 배치합니다.

### 3. 상태 관리 및 Hoisting
- **Stateless 지향**: 가능한 한 상태가 없는 컴포저블을 만듭니다. 상태는 파라미터로 전달받고, 이벤트는 콜백을 통해 위로 전달합니다.
- **상태 끌어올리기 (Hoisting)**:
  - 복잡한 상태는 `[ComponentName]State` 형태의 클래스나 인터페이스로 캡슐화합니다.
  - 단일 진실 공급원(Single Source of Truth)을 유지합니다.

## 단방향 데이터 흐름 (UDF)

UDF는 상태는 아래로 흐르고 이벤트는 위로 흐르는 패턴입니다.

1.  **Event**: 사용자의 액션(클릭 등)을 UI가 ViewModel에 알립니다.
2.  **Update**: ViewModel이 이벤트를 처리하고 비즈니스 로직(Domain/Data 계층)과 상호작용하여 상태를 업데이트합니다.
3.  **State**: 업데이트된 상태는 `StateFlow` 등을 통해 노출되며, UI는 이를 구독하여 다시 렌더링(Recomposition)됩니다.

## Preview 작성 가이드
- 각 컴포저블은 다양한 상태(성공, 에러, 로딩 등)를 시각적으로 확인할 수 있도록 `@Preview`를 작성해야 합니다.
- 테마(`SpeedoTheme`)를 적용하여 실제 앱과 동일한 스타일로 확인하세요.
