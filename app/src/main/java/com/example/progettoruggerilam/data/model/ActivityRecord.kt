package com.example.progettoruggerilam.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_records",
    indices = [Index(value = ["timestamp", "activityName"], unique = true)]  // Indice unico per timestamp e activityName
)
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,        // ID univoco della registrazione
    val userId: Long,                                         // ID dell'utente associato
    var activityName: String,                                 // Nome dell'attività (es: camminata, guida)
    val timestamp: Long,                                      // Timestamp di inizio attività
    var steps: Int,                                           // Numero di passi registrati
    var endTime: Long? = null,                                // Timestamp di inizio attività
) {

    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - timestamp

    init {
        require(steps >= 0) { "Steps cannot be negative" }
    }
}

