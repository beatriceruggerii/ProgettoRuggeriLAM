package com.example.progettoruggerilam.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.progettoruggerilam.util.GeofenceEntity
import com.example.progettoruggerilam.repository.GeofenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GeofenceViewModel(
    application: Application,
    private val geofenceRepository: GeofenceRepository
) : AndroidViewModel(application) {

    val geofences: Flow<List<GeofenceEntity>> = geofenceRepository.getAllGeofences()

    fun addGeofence(latitude: Double, longitude: Double, radius: Float, name: String) {
        val sharedPref = getApplication<Application>().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getLong("user_id", -1L)

        if (userId == -1L) {
            Log.e("GeofenceViewModel", "User ID not found")
            return
        }

        val geofence = GeofenceEntity(
            id = 0,
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            name = name,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            geofenceRepository.insertGeofence(geofence)
        }
    }

    fun deleteGeofence(geofence: GeofenceEntity) = viewModelScope.launch {
        geofenceRepository.deleteGeofence(geofence)
    }

    fun deleteAllGeofences() = viewModelScope.launch {
        geofenceRepository.deleteAllGeofences()
    }
}

