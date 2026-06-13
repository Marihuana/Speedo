package kr.yooreka.speedo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kr.yooreka.speedo.R
import kr.yooreka.speedo.ui.MainActivity

class RecordingService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_START_RECORDING) {
            startForegroundService()
        } else if (intent?.action == ACTION_STOP_RECORDING) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    "Ride Recording",
                    NotificationManager.IMPORTANCE_LOW,
                )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.service_title))
                .setContentText(getString(R.string.service_desc))
                .setSmallIcon(R.drawable.ic_monitor) // ensure this exists or use R.mipmap.ic_launcher
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
    }
}
