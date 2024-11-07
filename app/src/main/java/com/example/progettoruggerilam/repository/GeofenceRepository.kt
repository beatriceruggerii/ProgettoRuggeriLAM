package com.example.progettoruggerilam.repository

import com.example.progettoruggerilam.data.dao.GeofenceDao
import com.example.progettoruggerilam.util.GeofenceEntity
import kotlinx.coroutines.flow.Flow

class GeofenceRepository(
    private val geofenceDao: GeofenceDao
) {
    fun getAllGeofences(): Flow<List<GeofenceEntity>> = geofenceDao.getAllGeofences()

    suspend fun insertGeofence(geofence: GeofenceEntity) {
        geofenceDao.insertGeofence(geofence)
    }

    suspend fun deleteGeofence(geofence: GeofenceEntity) {
        geofenceDao.deleteGeofence(geofence)
    }

    suspend fun deleteAllGeofences() {
        geofenceDao.deleteAllGeofences()
    }
}
