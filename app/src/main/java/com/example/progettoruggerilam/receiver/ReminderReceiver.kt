// ReminderReceiver.kt
package com.example.progettoruggerilam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.progettoruggerilam.worker.ActivityReminderWorker

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Avvia il Worker per inviare la notifica
        val workRequest = OneTimeWorkRequestBuilder<ActivityReminderWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
