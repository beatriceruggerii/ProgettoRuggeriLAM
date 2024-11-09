package com.example.progettoruggerilam.data.dao

import androidx.room.*
import com.example.progettoruggerilam.util.GeofenceEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface GeofenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceEntity)

    @Query("SELECT * FROM geofences ORDER BY createdAt DESC")
    fun getAllGeofences(): Flow<List<GeofenceEntity>>

    @Query("SELECT * FROM geofences WHERE id = :id LIMIT 1")
    suspend fun getGeofenceById(id: Int): GeofenceEntity?

    @Query("SELECT * FROM geofences WHERE name = :name LIMIT 1")
    suspend fun getGeofenceByName(name: String): GeofenceEntity? // Query per ottenere un marker per nome

    @Delete
    suspend fun deleteGeofence(geofence: GeofenceEntity)

    @Query("SELECT * FROM geofences WHERE userId = :userId")
    fun getGeofencesByUserId(userId: Long): Flow<List<GeofenceEntity>>

    @Query("DELETE FROM geofences")
    suspend fun deleteAllGeofences()

    @Update
    suspend fun updateGeofence(geofence: GeofenceEntity)
}
