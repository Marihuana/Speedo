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

## 컴포넌트 범위(Scope) 선택 가이드

모든 의존성을 무조건 `SingletonComponent`로 묶지 마세요. 앱 프로세스 전체 수명에 불필요하게 객체를 유지하면 메모리 효율이 저하됩니다. **객체의 실제 생명주기에 맞는 가장 좁은 범위**를 선택합니다.

| 컴포넌트 (`@InstallIn`) | 스코프 어노테이션 | 생명주기 | 적합한 대상 |
| --- | --- | --- | --- |
| `SingletonComponent` | `@Singleton` | 앱 프로세스 전체 | DB, DataStore, Retrofit, 앱 전역 상태를 가진 Repository |
| `ViewModelComponent` | `@ViewModelScoped` | ViewModel 수명 | 특정 화면 흐름에서만 쓰는 상태 보관 객체 |
| `ActivityRetainedComponent` | `@ActivityRetainedScoped` | 구성 변경에도 유지(Activity) | 여러 ViewModel이 공유하되 Activity 범위인 객체 |
| `ActivityComponent` | `@ActivityScoped` | Activity 수명 | Activity 컨텍스트가 필요한 객체 |

```kotlin
// 앱 전역에서 하나만 필요한 무거운 객체 → Singleton
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpeedoDatabase = /* ... */
}

// 특정 화면 흐름에서만 살아있으면 충분한 객체 → ViewModelScoped
@Module
@InstallIn(ViewModelComponent::class)
object SessionModule {
    @Provides
    @ViewModelScoped
    fun provideRecordingSessionTracker(): RecordingSessionTracker = RecordingSessionTracker()
}
```

> 원칙: **상태가 없는(stateless) 객체는 보통 스코프가 불필요**합니다. 스코프는 "동일 인스턴스를 공유해야 할 때"만 부여하고, 공유 범위가 좁을수록 좁은 컴포넌트를 선택합니다.

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
