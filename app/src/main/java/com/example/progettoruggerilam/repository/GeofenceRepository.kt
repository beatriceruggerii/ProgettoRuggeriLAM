package com.example.progettoruggerilam.repository

import com.example.progettoruggerilam.data.dao.GeofenceDao
import com.example.progettoruggerilam.util.GeofenceEntity
import kotlinx.coroutines.flow.Flow

class GeofenceRepository(
    private val geofenceDao: GeofenceDao
) {
    fun getAllGeofences(): Flow<List<GeofenceEntity>> = geofenceDao.getAllGeofences()

    fun getGeofencesByUserId(userId: Long): Flow<List<GeofenceEntity>> = geofenceDao.getGeofencesByUserId(userId)

    suspend fun insertGeofence(geofence: GeofenceEntity) {
        geofenceDao.insertGeofence(geofence)
    }

    suspend fun deleteGeofence(geofence: GeofenceEntity) {
        geofenceDao.deleteGeofence(geofence)
    }

    suspend fun deleteAllGeofences() {
        geofenceDao.deleteAllGeofences()
    }

    suspend fun updateGeofence(geofence: GeofenceEntity) {
        geofenceDao.updateGeofence(geofence)
    }

    suspend fun getGeofenceById(id: Int): GeofenceEntity? = geofenceDao.getGeofenceById(id)

    suspend fun getGeofenceByName(name: String): GeofenceEntity? = geofenceDao.getGeofenceByName(name)
}
