# Dependency Injection (Hilt) 가이드

Hilt는 안드로이드에서 의존성 주입(DI)을 구현하기 위해 구글이 권장하는 라이브러리입니다.

## 주요 어노테이션

- `@HiltAndroidApp`: Application 클래스에 추가하여 Hilt 코드 생성을 트리거합니다.
- `@AndroidEntryPoint`: Activity, Fragment, View, Service, BroadcastReceiver 등 안드로이드 컴포넌트에 의존성을 주입할 수 있게 합니다.
- `@HiltViewModel`: ViewModel 클래스에 추가하여 Hilt가 ViewModel 인스턴스를 관리하고 주입할 수 있게 합니다.
- `@Inject`: 의존성을 주입받을 생성자나 필드에 사용합니다.

## 모듈 구성 (`@Module`)

인터페이스나 외부 라이브러리 클래스와 같이 생성자를 직접 수정할 수 없는 경우 모듈을 사용합니다.

### 1. `@InstallIn`
- 모듈이 어느 Hilt 컴포넌트에 설치될지 지정합니다. (예: `SingletonComponent`, `ActivityComponent`, `ViewModelComponent`)

### 2. `@Binds` vs `@Provides`
- **`@Binds`**: 인터페이스의 구현체를 바인딩할 때 사용합니다. 추상 함수로 작성합니다.
- **`@Provides`**: 클래스 인스턴스를 직접 생성해야 하거나 외부 라이브러리 객체를 제공할 때 사용합니다.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindUserRepository(
        impl: UserRepositoryImpl
    ): UserRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.example.com")
            .build()
    }
}
```
