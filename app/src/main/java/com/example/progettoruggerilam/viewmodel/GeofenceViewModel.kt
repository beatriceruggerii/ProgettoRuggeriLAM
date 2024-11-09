package com.example.progettoruggerilam.viewmodel

import android.app.Application
import android.content.Context
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

    fun getGeofencesByUserId(userId: Long): Flow<List<GeofenceEntity>> = geofenceRepository.getGeofencesByUserId(userId)

    fun addGeofence(latitude: Double, longitude: Double, radius: Float, name: String) {
        val userId = getUserId() // Puoi implementare questa funzione per recuperare l'ID utente dal contesto
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

    fun updateGeofence(geofence: GeofenceEntity) = viewModelScope.launch {
        geofenceRepository.updateGeofence(geofence)
    }

    suspend fun getGeofenceById(id: Int): GeofenceEntity? = geofenceRepository.getGeofenceById(id)

    suspend fun getGeofenceByName(name: String): GeofenceEntity? = geofenceRepository.getGeofenceByName(name)

    private fun getUserId(): Long {
        val sharedPref = getApplication<Application>().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getLong("user_id", -1L)
    }
}
