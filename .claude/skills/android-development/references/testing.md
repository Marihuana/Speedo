# 테스트 작성 가이드

Google의 [테스트 가이드](https://developer.android.com/training/testing)와 Clean Architecture를 따라, **순수 로직은 빠른 JVM 단위 테스트로** 검증하는 것을 기본으로 합니다. 도메인 레이어가 순수 Kotlin이어야 하는 이유가 바로 테스트 용이성입니다(`domain_layer.md` 참고).

## 테스트 피라미드

| 계층 | 위치 | 대상 | 의존성 처리 |
| --- | --- | --- | --- |
| **단위 (다수)** | `app/src/test` | UseCase, Repository 로직, 순수 계산기(예: `SpeedResolver`, `RideDistanceTracker`) | Fake / 가짜 구현 주입 |
| **계측 (소수)** | `app/src/androidTest` | DAO(Room), Compose UI, 권한/Service 통합 | 실기기/에뮬레이터 |

> 가능한 한 **단위 테스트로 내려서** 검증합니다. 안드로이드 프레임워크에 의존하지 않는 로직은 `src/test`에서 빠르게 돌립니다.

## 1. 의존성은 Fake로 (Mockito보다 Fake 선호)

SOLID의 DIP 덕분에 Repository는 인터페이스(`domain/repository`)에 의존합니다. 테스트에서는 **인터페이스를 구현한 Fake**를 주입합니다. Mock 프레임워크보다 동작이 명확하고 리팩토링에 강합니다.

```kotlin
class FakeTelemetryRepository : TelemetryRepository {
    private val _isRecording = MutableStateFlow(false)
    override val isRecording: Flow<Boolean> = _isRecording
    override fun startRecording() { _isRecording.value = true }
    override fun stopRecording() { _isRecording.value = false }
    // ...
}
```

## 2. 코루틴 / suspend 테스트

`kotlinx-coroutines-test`의 `runTest`를 사용하고, ViewModel 테스트에서는 `Dispatchers.setMain`으로 메인 디스패처를 교체합니다.

```kotlin
@Test
fun `기록 시작 시 isRecording이 true가 된다`() = runTest {
    val repo = FakeTelemetryRepository()
    repo.startRecording()
    assertTrue(repo.isRecording.first())
}
```

## 3. Flow 테스트는 Turbine

`StateFlow`/`Flow` 방출 순서 검증은 [Turbine](https://github.com/cashapp/turbine)으로 명시적으로 확인합니다.

```kotlin
@Test
fun `uiState는 기본값 후 갱신값을 방출한다`() = runTest {
    viewModel.uiState.test {
        assertEquals(DashBoardState(), awaitItem())
        // ... 트리거
        assertEquals("60", awaitItem().speed)
        cancelAndIgnoreRemainingEvents()
    }
}
```

## 4. Room DAO 테스트 (계측)

DAO는 인메모리 DB(`Room.inMemoryDatabaseBuilder`)로 `androidTest`에서 검증합니다.

## 명명 규칙
- 테스트 함수는 백틱으로 **행동을 한국어/문장으로** 서술합니다: `` `조건_when_then` ``.
- Given-When-Then 구조로 본문을 구성해 가독성을 높입니다.

## 체크리스트
- 새 UseCase/Repository 로직에는 **단위 테스트를 동반**한다.
- 프레임워크 의존이 없는 로직을 `androidTest`에 두지 않는다(느려짐).
- 테스트는 Fake로 결정적(deterministic)으로 만든다 — 실제 센서/GPS/시간에 의존하지 않는다.
