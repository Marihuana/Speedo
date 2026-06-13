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

## 흐름 관리
- Repository는 보통 데이터를 `Flow`로 노출하여 데이터 변화를 실시간으로 감지할 수 있게 합니다.

```kotlin
interface NewsRepository {
    fun getNewsStream(): Flow<List<Article>>
    suspend fun refreshNews()
}
```
