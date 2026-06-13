# Kotlin Style & Concurrency 가이드

이 문서는 안드로이드 개발 시 권장되는 코틀린 스타일과 비동기 처리(Coroutine/Flow) 규칙을 설명합니다.

## Kotlin 코딩 컨벤션
- 기본적으로 [Kotlin Official Style Guide](https://kotlinlang.org/docs/coding-conventions.html)를 따릅니다.
- **불변성 지향**: 가능한 한 `val`을 사용하고, 가변성이 필요한 경우에만 `var`를 고려합니다.
- **확장 함수 활용**: 특정 클래스에 종속되지 않는 유틸리티성 로직은 확장 함수로 분리하여 가독성을 높입니다.

## Coroutine & Flow 활용 규칙

### 1. Main-Safety 원칙
- **ViewModel**: 모든 작업은 UI 스레드(Main)에서 호출하기 안전해야 합니다. ViewModel은 `viewModelScope`를 사용하여 코루틴을 실행합니다.
- **데이터 레이어**: 네트워크 요청이나 DB 작업과 같은 무거운 작업은 반드시 적절한 Dispatcher(`Dispatchers.IO`)로 전환하여 실행해야 합니다.

```kotlin
// ViewModel 예시
class MyViewModel(private val repository: MyRepository) : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            // repository에서 Main-safe하게 데이터를 가져옴
            val result = repository.getData() 
            _uiState.value = result
        }
    }
}

// Repository 예시
class MyRepositoryImpl : MyRepository {
    override suspend fun getData() = withContext(Dispatchers.IO) {
        // 무거운 작업 수행
    }
}
```

### 2. Flow 사용
- 스트림 데이터 처리를 위해 Flow를 적극 활용합니다.
- UI 레이어에서는 `collectAsStateWithLifecycle` 등을 사용하여 수명 주기에 안전하게 데이터를 수집합니다.

## 에러 핸들링
- `try-catch` 블록보다는 `Result` 클래스나 커스텀 에러 래퍼를 활용하여 에러 상태를 명시적으로 전달하는 방식을 권장합니다.
