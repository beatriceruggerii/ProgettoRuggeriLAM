package com.example.progettoruggerilam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.progettoruggerilam.worker.GeofenceTransitionsWorker
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    private val TAG = "GeofenceReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        // Verifica se l'evento è null o se ha errori
        if (geofencingEvent == null || geofencingEvent.hasError()) {
            val errorCode = geofencingEvent?.errorCode ?: "unknown"
            Log.e(TAG, "Errore evento Geofencing: $errorCode")
            return
        }

        // Ottieni il tipo di transizione del Geofence
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Ottieni la lista di geofences attivati
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences.isNullOrEmpty()) {
            Log.e(TAG, "Nessun geofence attivato")
            return
        }

        // Recupera l'ID del primo geofence (per scopi di log e tracciamento)
        val requestId = triggeringGeofences[0].requestId

        // Log delle transizioni per monitorare l'evento
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.i(TAG, "Entrato nel geofence: $requestId")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.i(TAG, "Uscito dal geofence: $requestId")
            }
            else -> {
                Log.e(TAG, "Transizione Geofence non gestita: $geofenceTransition")
                return
            }
        }

        // Avvia GeofenceTransitionsWorker per gestire l'evento in background
        enqueueGeofenceWorker(context, geofenceTransition, requestId)
    }

    // Metodo per avviare il GeofenceTransitionsWorker
    private fun enqueueGeofenceWorker(context: Context, transitionType: Int, requestId: String) {
        val workRequest = OneTimeWorkRequest.Builder(GeofenceTransitionsWorker::class.java)
            .setInputData(
                workDataOf(
                    "transitionType" to transitionType,
                    "requestId" to requestId
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
