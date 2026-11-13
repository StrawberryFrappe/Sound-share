package io.github.strawberryfrappe.soundshare

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.github.strawberryfrappe.soundshare.ui.MainActivity

private const val CHANNEL_ID = "streaming"

/** The ongoing notification a service shows while it shares or listens, with a Stop button. */
fun Service.streamingNotification(title: String, stopIntent: Intent): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val open = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )
    val stop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentIntent(open)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .addAction(0, getString(R.string.notification_stop), stop)
        .build()
}

/**
 * Keeps the CPU awake and Wi-Fi out of its power-saving mode while audio is
 * streaming. Power saving delivers packets in clumps, which is what makes
 * streamed audio stutter when the screen turns off.
 */
class StreamingLocks(context: Context, tag: String) {
    private val wakeLock = context.getSystemService(PowerManager::class.java)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "soundshare:$tag")

    @Suppress("DEPRECATION") // The replacement only works while the app is on screen.
    private val wifiLock = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "soundshare:$tag")

    @SuppressLint("WakelockTimeout") // Held for as long as the user keeps streaming.
    fun acquire() {
        if (!wakeLock.isHeld) wakeLock.acquire()
        if (!wifiLock.isHeld) wifiLock.acquire()
    }

    fun release() {
        if (wakeLock.isHeld) wakeLock.release()
        if (wifiLock.isHeld) wifiLock.release()
    }
}
