package com.example.progettoruggerilam.service


import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.progettoruggerilam.worker.ActivityReminderWorker
import com.google.android.gms.location.ActivityRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit

class ActivityReminderService : Service() {

    private lateinit var pendingIntent: PendingIntent
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        //setupActivityTransitions()
        setNotification()
        setOneNotification()
        Log.w("ATTIVAZIONE SERVICE", "ATTIVAZIONE SERVICE !!!") // stampa controllo
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /*
    private fun setupActivityTransitions() {
        serviceScope.launch(Dispatchers.IO) {
            val transitions = mutableListOf<ActivityTransition>()

            val activities = listOf(
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.RUNNING,
                DetectedActivity.STILL,
                DetectedActivity.WALKING
            )

            for (activity in activities) {
                transitions += ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()

                transitions += ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            }

            val request = ActivityTransitionRequest(transitions)

            // Creo il PendingIntent
            val intent = Intent(this@ActivityMonitoringService, ActivityTransitionReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(
                this@ActivityMonitoringService,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Richiesta aggiornamenti delle transizioni delle attività
            try {
                val task = ActivityRecognition.getClient(this@ActivityMonitoringService)
                    .requestActivityTransitionUpdates(request, pendingIntent)

                task.addOnSuccessListener {
                    Log.d("ActivityMonitoringService", "Successfully registered for activity transitions")
                }

                task.addOnFailureListener { e: Exception ->
                    Log.e("ActivityMonitoringService", "Failed to register for activity transitions", e)
                }
            } catch (e: SecurityException) {
                Log.e("ActivityMonitoringService", "Permission not granted for activity recognition", e)
            }
        }
    }
    */

    private fun setNotification() {
        val workRequest = PeriodicWorkRequestBuilder<ActivityReminderWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ActivityReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Interrompo activity transitions quandp si chiude il service
        if (this::pendingIntent.isInitialized) {
            try {
                val task = ActivityRecognition.getClient(this).removeActivityTransitionUpdates(pendingIntent)

                task.addOnSuccessListener {
                    Log.d("ActivityMonitoringService", "Successfully unregistered for activity transitions")
                }

                task.addOnFailureListener { e: Exception ->
                    Log.e("ActivityMonitoringService", "Failed to unregister for activity transitions", e)
                }
            } catch (e: SecurityException) {
                Log.e("ActivityMonitoringService", "Permission not granted for activity recognition", e)
            }
        }
    }

    private fun setOneNotification() {
        val workOneRequest = OneTimeWorkRequestBuilder<ActivityReminderWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "OneTimeActivityReminder",
            ExistingWorkPolicy.KEEP,
            workOneRequest
        )
    }


}