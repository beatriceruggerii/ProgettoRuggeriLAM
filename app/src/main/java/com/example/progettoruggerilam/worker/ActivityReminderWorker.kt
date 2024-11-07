package com.example.progettoruggerilam.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.progettoruggerilam.R
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

class ActivityReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val CHANNEL_ID = "activity_reminder_channel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "ActivityReminderWorker"

        // Metodo statico per inviare la notifica
        fun sendNotification(context: Context) {
            // Verifica e richiedi permesso per le notifiche su Android 13+ (API 33 e successive)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Permesso per le notifiche non concesso.")
                return
            }

            val notificationManager = NotificationManagerCompat.from(context)

            // Crea il canale di notifica
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Activity Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Promemoria per registrare l'attività o fare più passi"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            // Crea e invia la notifica
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_alert)
                .setContentTitle("Un po' di movimento?")
                .setContentText("Registra la tua attività o aggiungi qualche passo!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visibilità su schermata di blocco
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun doWork(): Result {
        sendNotification(applicationContext)
        return Result.success()
    }
}
