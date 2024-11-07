package com.example.progettoruggerilam.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.progettoruggerilam.data.model.ActivityRecord
import com.example.progettoruggerilam.repository.ActivityRecordRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class ActivityViewModel(application: Application, private val repository: ActivityRecordRepository) : AndroidViewModel(application) {

    fun deleteRecordsByUserId(userId: Long) {
        viewModelScope.launch {
            repository.deleteRecordsByUserId(userId)
        }
    }

    // Funzione per ottenere le attività di un utente per tutto il mese corrente
    fun getRecordsByUserForMonth(userId: Long): LiveData<List<ActivityRecord>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1) // Inizio mese
        val startOfMonth = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) // Fine mese
        val endOfMonth = calendar.timeInMillis

        return repository.getRecordsByUserAndDate(userId, startOfMonth, endOfMonth)
    }

    fun getActivitiesByDateAndUser(date: String, userId: Long): LiveData<List<ActivityRecord>> {
        return repository.getActivitiesByDateAndUser(date, userId)
    }

    fun deleteUserAccountById(userId: Long) {
        viewModelScope.launch {
            repository.deleteUserAccountById(userId)
        }
    }

    fun insert(activityName: String, steps: Int, timestamp: Long, endTime: Long) {
        viewModelScope.launch {
            val sharedPref = getApplication<Application>().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val userId = sharedPref.getLong("user_id", -1L)

            if (userId != -1L) {
                val newActivity = ActivityRecord(
                    userId = userId,
                    activityName = activityName,
                    steps = steps,
                    timestamp = timestamp,  // Usa il tempo di inizio come timestamp
                    endTime = endTime        // Usa il tempo di fine
                )
                repository.insert(newActivity)
            } else {
                Log.e("ActivityInsert", "User ID non trovato, impossibile salvare l'attività")
            }
        }
    }
}






