# Navigation (Navigation Compose) 가이드

화면 간 이동은 [Navigation Compose](https://developer.android.com/develop/ui/compose/navigation)로 구현합니다. 단일 `NavHost`를 단일 Activity(`MainActivity`)에 두는 **Single-Activity 구조**를 따릅니다.

## 핵심 원칙

1. **Single-Activity / Single NavHost**: 화면은 Activity가 아니라 `composable` 목적지(destination)로 구성합니다.
2. **라우트 중앙 관리**: 라우트 문자열을 화면에 흩뿌리지 않고 한 곳(sealed class 또는 type-safe route)에 정의합니다. 매직 스트링을 지양합니다(Clean Code).
3. **NavController는 위로**: `NavController`를 하위 컴포저블에 내려주지 않습니다. 대신 **이동 이벤트를 람다(`onNavigateToX: () -> Unit`)로 끌어올려(hoist)** 화면을 네비게이션으로부터 분리합니다(SOLID-SRP, 테스트·프리뷰 용이).
4. **ViewModel 주입**: 각 목적지에서 `hiltViewModel()`로 화면 스코프 ViewModel을 주입합니다.

## 라우트 정의 (type-safe 권장)

Navigation 2.8+의 type-safe API(`@Serializable` 객체)를 우선 사용합니다.

```kotlin
@Serializable data object Dashboard
@Serializable data object Records
@Serializable data class RideDetail(val rideId: Long)
```

## NavHost 구성

```kotlin
@Composable
fun SpeedoNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Dashboard) {
        composable<Dashboard> {
            val vm: DashBoardViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            DashBoardScreen(
                state = state,
                onNavigateToRecords = { navController.navigate(Records) },
            )
        }
        composable<RideDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<RideDetail>()
            // args.rideId 사용
        }
    }
}
```

## 화면 분리 원칙

```kotlin
// ✅ 화면은 이동 의도만 콜백으로 노출 — NavController를 모른다
@Composable
fun DashBoardScreen(state: DashBoardState, onNavigateToRecords: () -> Unit) { /* ... */ }

// ❌ 지양: 화면이 NavController에 직접 의존 → 프리뷰/테스트 곤란, 결합도 상승
@Composable
fun DashBoardScreen(navController: NavController) { /* ... */ }
```

## 체크리스트
- 라우트가 한 곳에 정의되어 있고 매직 스트링이 없는가.
- 화면 컴포저블이 `NavController` 대신 이동 콜백을 받는가.
- 인자 전달에 type-safe route(`toRoute<T>()`)를 쓰는가.
- 목적지마다 `hiltViewModel()`로 ViewModel을 주입하고 `collectAsStateWithLifecycle()`로 상태를 수집하는가(`ui_layer.md` 참고).
