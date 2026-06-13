# 런타임 권한 (Runtime Permissions) 가이드

Speedo는 GPS 속도/주행 기록이 핵심이므로 **위치 권한**과 **알림 권한**의 런타임 요청은 가장 중요한 흐름입니다. Google 권장 패턴([Request runtime permissions](https://developer.android.com/training/permissions/requesting))을 따릅니다.

## 핵심 원칙

1. **선언과 요청 분리**: Manifest의 `<uses-permission>` 선언만으로는 부족합니다. 위험 권한(dangerous)은 **런타임에 사용자 동의**를 받아야 합니다.
2. **최소 권한**: 필요한 시점에, 필요한 권한만 요청합니다. 정확한 위치가 필요 없으면 `ACCESS_COARSE_LOCATION`만 요청합니다.
3. **거부 대응**: 거부/영구 거부 상태를 구분해 처리하고, 권한 없이도 앱이 크래시 없이 동작(degrade)하도록 설계합니다.
4. **계층 분리(SOLID-SRP)**: 권한 요청은 **UI 레이어의 책임**입니다. ViewModel/Domain/Data는 권한을 직접 요청하지 않고, "권한 보유 여부"만 상태로 전달받습니다.

## 대상 권한 (Speedo)

| 권한 | 종류 | 요청 시점 |
| --- | --- | --- |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | dangerous | 주행 기록/속도 측정 시작 직전 |
| `POST_NOTIFICATIONS` (API 33+) | dangerous | Foreground Service 알림 표시 전 |
| `ACCESS_BACKGROUND_LOCATION` | dangerous (특수) | 백그라운드 위치가 꼭 필요할 때만, **별도 단계**로 |

> `ACCESS_BACKGROUND_LOCATION`은 포그라운드 권한 승인 **이후 별도 요청**해야 하며, 가능하면 Foreground Service(`foregroundServiceType="location"`)로 대체해 요청을 피합니다. 자세한 서비스 구성은 `service_layer.md` 참고.

## 표준 패턴 (Compose)

`rememberLauncherForActivityResult` + `ActivityResultContracts`를 사용합니다. `Activity.requestPermissions` 직접 호출은 지양합니다.

```kotlin
@Composable
fun rememberLocationPermissionRequest(
    onResult: (granted: Boolean) -> Unit,
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // FINE 또는 COARSE 중 하나라도 허용되면 위치 사용 가능
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onResult(granted)
    }
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}
```

알림 권한은 버전 가드를 둡니다.

```kotlin
// API 33 미만에서는 런타임 요청이 불필요(항상 허용)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

## Rationale(근거 설명) 처리

`shouldShowRequestPermissionRationale`가 `true`면, 권한이 왜 필요한지 설명하는 UI(다이얼로그)를 먼저 보여준 뒤 다시 요청합니다. 영구 거부 상태에서는 설정 화면으로 유도합니다.

```kotlin
when {
    granted -> startRecording()
    activity.shouldShowRequestPermissionRationale(perm) -> showRationaleDialog()
    else -> showGoToSettingsDialog() // 영구 거부 → 앱 설정으로 이동
}
```

## 체크리스트
- **Manifest 선언**: 사용하는 모든 위험 권한을 `<uses-permission>`으로 선언했는가.
- **버전 가드**: `POST_NOTIFICATIONS`(33+), `ACCESS_BACKGROUND_LOCATION`(별도 단계)를 SDK 버전으로 분기했는가.
- **계층 분리**: 권한 요청 로직이 UI 레이어에만 있고, ViewModel은 `Boolean` 상태만 받는가.
- **Degrade**: 권한 거부 시 크래시 없이 대체 동작/안내를 제공하는가.
- **Rationale**: 거부 후 재요청 시 근거 설명 UI를 거치는가.
