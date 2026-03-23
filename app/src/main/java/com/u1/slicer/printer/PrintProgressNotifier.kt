package com.u1.slicer.printer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.u1.slicer.MainActivity
import com.u1.slicer.network.PrinterStatus

internal object PrintProgressNotifier {
    private const val CHANNEL_ID = "print_progress"
    private const val NOTIFICATION_ID = 2

    fun update(context: Context, status: PrinterStatus) {
        if (!shouldShow(status)) {
            clear(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle(titleFor(status))
                .setContentText(textFor(status))
                .setStyle(NotificationCompat.BigTextStyle().bigText(textFor(status)))
                .setContentIntent(mainActivityIntent(context))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSilent(true)
                .setProgress(100, status.progressPercent.coerceIn(0, 100), status.state == "printing" && status.progressPercent == 0)
                .build()
        )
    }

    fun clear(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    internal fun shouldShow(status: PrinterStatus): Boolean =
        status.isPrinting || status.isPaused

    internal fun titleFor(status: PrinterStatus): String = when {
        status.isPaused -> "Print paused"
        else -> "Printing ${status.progressPercent}%"
    }

    internal fun textFor(status: PrinterStatus): String {
        val name = status.filename.ifBlank { "Current print" }
        return when {
            status.isPaused -> "$name is paused at ${status.progressPercent}%"
            else -> "$name in progress"
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Print progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing printer progress while a print is active"
                setShowBadge(false)
            }
        )
    }

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
