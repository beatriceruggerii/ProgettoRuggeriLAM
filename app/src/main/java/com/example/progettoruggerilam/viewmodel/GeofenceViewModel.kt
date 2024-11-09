package com.example.progettoruggerilam.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.progettoruggerilam.util.GeofenceEntity
import com.example.progettoruggerilam.repository.GeofenceRepository
import kotlinx.coroutines.flow.Flow


class GeofenceViewModel(
    application: Application,
    private val geofenceRepository: GeofenceRepository
) : AndroidViewModel(application) {

    val geofences: Flow<List<GeofenceEntity>> = geofenceRepository.getAllGeofences()

}
