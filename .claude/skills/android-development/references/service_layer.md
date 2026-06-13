# Service Layer (Foreground Service) 가이드

주행 기록처럼 화면이 꺼져도 계속 동작해야 하는 장시간 작업은 **Foreground Service**로 구현합니다. Speedo의 `RecordingService`가 표준 예시입니다.

## 핵심 원칙

1.  **Foreground Service 사용**: 사용자가 인지할 수 있는 장시간 작업(위치/센서 기록 등)은 반드시 알림(Notification)을 동반한 Foreground Service로 실행합니다.
2.  **Action 기반 제어**: 시작/중지는 `Intent`의 `action`으로 구분하고, 상수는 `companion object`에 정의합니다.
3.  **Bound 불필요 시 `onBind`는 null**: 단순 시작/중지만 필요한 경우 `onBind`에서 `null`을 반환합니다.
4.  **`START_STICKY`**: 시스템이 서비스를 종료해도 재시작되도록 반환합니다(기록 연속성).
5.  **`foregroundServiceType` 명시 (Android 14 / API 34+ 필수)**: Manifest의 `<service>`에 타입을 선언하고, `startForeground()` 호출 시에도 동일한 타입을 런타임에 전달해야 합니다. 누락 시 Android 14 이상 기기에서 `MissingForegroundServiceTypeException` 크래시가 발생합니다.

## 구현 패턴

```kotlin
class RecordingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startForegroundService()
            ACTION_STOP_RECORDING -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "recording_channel"
        // Android O(API 26) 이상은 NotificationChannel 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Ride Recording", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.service_title))
            .setContentText(getString(R.string.service_desc))
            .setSmallIcon(R.drawable.ic_monitor)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        // Android 14(API 34) 이상: startForeground에 타입을 명시해야 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
    }
}
```

## 체크리스트

- **Manifest 등록**: `<service>` 선언과 `FOREGROUND_SERVICE` (및 위치 기록 시 `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`) 권한을 추가합니다.
- **`foregroundServiceType` 선언/전달**: Manifest `<service android:foregroundServiceType="location" />`로 선언하고, API 34+에서는 `startForeground(id, notification, FOREGROUND_SERVICE_TYPE_LOCATION)`로 런타임 타입까지 전달합니다.
- **알림 채널**: API 26 이상에서는 `NotificationChannel`을 반드시 먼저 생성합니다.
- **PendingIntent**: `FLAG_IMMUTABLE`을 포함합니다 (API 31+ 필수).
- **문자열 리소스**: 알림 제목/내용은 하드코딩하지 말고 `R.string.*`로 관리합니다.
- **서비스 시작**: UI/ViewModel에서 직접 `startService`/`startForegroundService`를 호출하지 말고, 가능하면 Repository나 별도 컨트롤러를 통해 트리거하여 계층 분리를 유지합니다.
