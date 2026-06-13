# Domain Layer 가이드

도메인 레이어는 UI 레이어와 데이터 레이어 사이에 위치하며, 복잡한 비즈니스 로직이나 여러 ViewModel에서 재사용되는 로직을 캡슐화합니다.

## Use Case (Interactor) 원칙

### 1. 단일 책임 원칙 (Single Responsibility)
- 각 Use Case는 하나의 명확한 비즈니스 작업을 수행해야 합니다. (예: `GetUserProfileUseCase`, `UpdateTirePressureUseCase`)
- 클래스 이름은 수행하는 작업을 동사 위주로 명확하게 표현합니다.

### 2. 프레임워크 독립성
- 도메인 레이어는 안드로이드 프레임워크(Context, ViewModel 등)에 의존해서는 안 됩니다.
- **순수 Kotlin**으로 작성하여 테스트 용이성과 이식성을 높입니다.

### 3. 구조
- 일반적으로 `invoke` 연산자 오버로딩을 사용하여 함수처럼 호출할 수 있게 만듭니다.
- 데이터 레이어의 Repository 인터페이스를 주입받아 사용합니다.

```kotlin
class GetDashboardDataUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    suspend operator fun invoke(): Result<DashboardData> {
        return repository.getDashboardData()
    }
}
```

## 사용 시기
- 로직이 복잡하여 ViewModel이 비대해질 때.
- 여러 화면(ViewModel)에서 동일한 비즈니스 로직이 반복될 때.
- 가독성을 높이고 비즈니스 규칙을 명확히 문서화하고 싶을 때.
