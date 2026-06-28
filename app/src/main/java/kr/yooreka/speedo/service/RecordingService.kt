package kr.yooreka.speedo.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kr.yooreka.speedo.R
import kr.yooreka.speedo.domain.repository.TelemetryRepository
import kr.yooreka.speedo.ui.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {
    @Inject
    lateinit var telemetryRepository: TelemetryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // 주행 종료 예상(F-18) 상태를 관찰해 백그라운드에서도 액션 알림으로 종료/계속을 묻는다.
        serviceScope.launch {
            telemetryRepository.autoStopSuggested.collect { suggested ->
                if (suggested) postAutoStopNotification() else cancelAutoStopNotification()
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startForegroundService()
            ACTION_STOP_RECORDING -> {
                cancelAutoStopNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            // 종료 예상 알림의 액션 버튼(백그라운드 대응, F-18).
            ACTION_STOP_RIDE -> {
                cancelAutoStopNotification()
                telemetryRepository.confirmAutoStop()
            }
            ACTION_CONTINUE_RIDE -> {
                cancelAutoStopNotification()
                telemetryRepository.continueRide()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    RECORDING_CHANNEL_ID,
                    getString(R.string.notif_channel_recording),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(this, RECORDING_CHANNEL_ID)
                .setContentTitle(getString(R.string.service_title))
                .setContentText(getString(R.string.service_desc))
                .setSmallIcon(R.drawable.ic_monitor)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    /** 주행 종료 예상(F-18) 액션 알림. 알림 권한이 있을 때만 표시한다. */
    @SuppressLint("MissingPermission") // hasNotificationPermission() 으로 직접 가드한다.
    private fun postAutoStopNotification() {
        if (!hasNotificationPermission()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    AUTO_STOP_CHANNEL_ID,
                    getString(R.string.notif_channel_auto_stop),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val openIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(this, AUTO_STOP_CHANNEL_ID)
                .setContentTitle(getString(R.string.auto_stop_notif_title))
                .setContentText(getString(R.string.auto_stop_notif_text))
                .setSmallIcon(R.drawable.ic_monitor)
                .setContentIntent(openIntent)
                .addAction(R.drawable.ic_stop, getString(R.string.auto_stop_notif_stop), servicePendingIntent(ACTION_STOP_RIDE, 1))
                .addAction(R.drawable.ic_play, getString(R.string.auto_stop_notif_continue), servicePendingIntent(ACTION_CONTINUE_RIDE, 2))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

        NotificationManagerCompat.from(this).notify(AUTO_STOP_NOTIFICATION_ID, notification)
    }

    private fun cancelAutoStopNotification() {
        NotificationManagerCompat.from(this).cancel(AUTO_STOP_NOTIFICATION_ID)
    }

    private fun servicePendingIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RecordingService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val AUTO_STOP_NOTIFICATION_ID = 2
        private const val RECORDING_CHANNEL_ID = "recording_channel"
        private const val AUTO_STOP_CHANNEL_ID = "auto_stop_channel"

        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val ACTION_STOP_RIDE = "ACTION_STOP_RIDE"
        const val ACTION_CONTINUE_RIDE = "ACTION_CONTINUE_RIDE"
    }
}
