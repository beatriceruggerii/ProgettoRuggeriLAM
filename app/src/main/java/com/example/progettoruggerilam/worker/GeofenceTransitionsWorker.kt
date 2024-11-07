package com.example.progettoruggerilam.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeofenceTransitionsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Recupera i dati passati dal WorkRequest
        val geofenceTransition = inputData.getInt("geofence_transition", -1)
        val triggeringGeofencesIds = inputData.getStringArray("triggering_geofences_ids")

        // Verifica che i dati siano validi
        if (geofenceTransition == -1 || triggeringGeofencesIds.isNullOrEmpty()) {
            return@withContext Result.failure()
        }

        // Gestisci le transizioni di geofence (entrata/uscita)
        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> handleGeofenceEnter(triggeringGeofencesIds)
            Geofence.GEOFENCE_TRANSITION_EXIT -> handleGeofenceExit(triggeringGeofencesIds)
            else -> {
                // Transizione non gestita
                return@withContext Result.failure()
            }
        }

        return@withContext Result.success()
    }

    private fun handleGeofenceEnter(geofencesIds: Array<String>) {
        geofencesIds.forEach { geofenceId ->
            // Gestisci l'entrata nel Geofence
            // Puoi fare un log o salvare i dati
        }
    }

    private fun handleGeofenceExit(geofencesIds: Array<String>) {
        geofencesIds.forEach { geofenceId ->
            // Gestisci l'uscita dal Geofence
            // Puoi fare un log o salvare i dati
        }
    }

    companion object {
        fun enqueueWork(context: Context, geofenceTransition: Int, triggeringGeofencesIds: Array<String>) {
            val workRequest = OneTimeWorkRequest.Builder(GeofenceTransitionsWorker::class.java)
                .setInputData(
                    workDataOf(
                        "geofence_transition" to geofenceTransition,
                        "triggering_geofences_ids" to triggeringGeofencesIds
                    )
                )
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

