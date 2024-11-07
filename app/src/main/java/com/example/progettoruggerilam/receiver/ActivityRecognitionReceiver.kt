package com.example.progettoruggerilam.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.progettoruggerilam.data.model.ActivityRecord
import com.example.progettoruggerilam.repository.ActivityRecordRepository
import com.example.progettoruggerilam.data.database.AppDatabase
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityRecognitionReceiver : BroadcastReceiver() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onReceive(context: Context, intent: Intent) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Verifica il permesso di localizzazione
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("ActivityRecognition", "Permesso di localizzazione non concesso")
            return
        }

        // Verifica se il risultato contiene un `ActivityTransitionResult`
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)

            result?.transitionEvents?.forEach { event ->
                val activityType = when (event.activityType) {
                    DetectedActivity.IN_VEHICLE -> "In Vehicle"
                    DetectedActivity.WALKING -> "Walking"
                    DetectedActivity.RUNNING -> "Running"
                    else -> "Unknown"
                }

                val transitionType = when (event.transitionType) {
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "Started"
                    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "Stopped"
                    else -> "Unknown Transition"
                }

                Log.d("ActivityRecognition", "$activityType $transitionType")

                // Recupera l'ID utente
                val userId = getUserId(context)
                if (userId > 0) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {

                            // Recupera la repository
                            val repository = ActivityRecordRepository(AppDatabase.getDatabase(context).activityRecordDao())

                            CoroutineScope(Dispatchers.IO).launch {
                                if (transitionType == "Started") {
                                    // Crea un nuovo record per l'attività che è iniziata
                                    val newActivityRecord = ActivityRecord(
                                        userId = userId,
                                        activityName = activityType,
                                        endTime = 0, // EndTime sarà aggiornato quando l'attività finirà
                                        steps = getStepCountFromService(context),  // Ottieni i passi dal servizio
                                        timestamp = System.currentTimeMillis(),
                                    )
                                    repository.insert(newActivityRecord)
                                } else if (transitionType == "Stopped") {
                                    // Aggiorna l'attività corrente con l'endTime e la posizione finale
                                    val ongoingActivity = repository.getOngoingActivity(userId)
                                    ongoingActivity?.let {
                                        it.endTime = System.currentTimeMillis()
                                        repository.update(it)
                                    }
                                }
                            }
                        } else {
                            Log.e("ActivityRecognition", "Impossibile ottenere la posizione corrente")
                        }
                    }
                } else {
                    Log.e("ActivityRecognition", "Invalid userId: $userId")
                }
            }
        } else {
            Log.e("ActivityRecognition", "No ActivityTransitionResult found")
        }
    }

    // Funzione per ottenere i passi reali dal servizio
    private fun getStepCountFromService(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("steps", 0)  // Restituisce il numero di passi salvato
    }

    // Funzione per ottenere l'ID utente dalle SharedPreferences
    private fun getUserId(context: Context): Long {
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", 0)
    }
}
