package com.example.progettoruggerilam.util

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "geofences")
data class GeofenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Long,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val name: String,
    val createdAt: Long
)
