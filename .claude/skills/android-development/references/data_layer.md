# Data Layer 가이드

데이터 레이어는 앱의 비즈니스 로직을 포함하고 애플리케이션 데이터를 관리합니다.

## Repository 패턴

### 1. 단일 진실 공급원 (Single Source of Truth)
- 데이터 레이어의 주요 역할은 다양한 데이터 소스(Network, Local DB, Cache)를 조율하여 하나의 신뢰할 수 있는 데이터를 제공하는 것입니다.

### 2. 구조
- **Repository**: 데이터 도메인별로 인터페이스를 정의하고 구현체를 만듭니다. (예: `UserRepository`, `WeatherRepository`)
- **Data Source**: 실제 데이터에 접근하는 하위 클래스입니다.
  - `RemoteDataSource`: Retrofit 등을 이용한 API 통신.
  - `LocalDataSource`: Room, DataStore 등을 이용한 로컬 저장.

## 오프라인 우선 (Offline-First) 전략
- 가능한 한 로컬 데이터를 먼저 보여주고, 백그라운드에서 네트워크 데이터를 업데이트하여 로컬 DB를 갱신하는 방식을 권장합니다.
- 네트워크가 없는 환경에서도 앱이 기본적으로 동작할 수 있도록 설계합니다.

## 함수 정의 규칙: Flow vs suspend

Repository 함수는 **"관찰"인지 "명령"인지**에 따라 반환 타입을 명확히 구분합니다. 단발성 액션까지 `Flow`로 노출하면 불필요한 보일러플레이트와 복잡한 구독 로직이 발생하므로 지양합니다.

| 성격 | 반환 타입 | 예시 |
| --- | --- | --- |
| **관찰 가능한 스트림** (시간에 따라 값이 변함) | `Flow<T>` | `getNewsStream()`, `observeRecordingState()`, 현재 속도/위치 스트림 |
| **단발성 명령/조회** (한 번 실행 후 종료) | `suspend fun` | `refreshNews()`, `startRecording()`, `calibrateZero()`(영점 보정), `saveRecord()` |

```kotlin
interface TelemetryRepository {
    // ✅ 관찰: 값이 계속 바뀌므로 Flow
    val isRecording: Flow<Boolean>
    fun observeSpeed(): Flow<Float>

    // ✅ 명령: 한 번 실행되고 끝나므로 suspend
    suspend fun startRecording()
    suspend fun stopRecording()
    suspend fun calibrateZero()
}
```

> 판단 기준: "이 값을 **계속 지켜봐야 하는가**(Flow)?" vs "이건 **한 번 시키고 끝나는 동작인가**(suspend)?"
