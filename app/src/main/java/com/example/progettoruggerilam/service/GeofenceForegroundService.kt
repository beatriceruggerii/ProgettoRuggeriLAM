package com.example.progettoruggerilam.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class GeofenceForegroundService : Service() {

    private lateinit var locationManager: LocationManager
    private val insideGeofence = mutableSetOf<Long>()
    private val db by lazy { AppDatabase.getDatabase(this) }

    companion object {
        private const val CHANNEL_ID = "GeofenceServiceChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        startLocationMonitoring()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geofence Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Monitoring Geofences")
            .setContentText("Geofence monitoring is active")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startLocationMonitoring() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("GeofenceService", "Location permission not granted.")
            stopSelf()
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L,
            10f,
            locationListener
        )
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentLocation = GeoPoint(location.latitude, location.longitude)
            checkGeofenceEntryExit(currentLocation)
        }
    }

    private fun checkGeofenceEntryExit(currentLocation: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Colleziona i geofence usando `collect` poiché è un Flow
                db.geofenceDao().getGeofencesByUserId(getUserId()).collect { geofences ->
                    geofences.forEach { geofence ->
                        // Assicurati che le proprietà siano accessibili e corrette
                        val geofenceLocation = GeoPoint(geofence.latitude, geofence.longitude)
                        val distance = currentLocation.distanceToAsDouble(geofenceLocation)

                        if (distance <= geofence.radius && !insideGeofence.contains(geofence.id.toLong())) {
                            insideGeofence.add(geofence.id.toLong())
                            withContext(Dispatchers.Main) {
                                sendGeofenceNotification("Sei entrato nell'area di interesse: ${geofence.name}")
                            }
                        } else if (distance > geofence.radius && insideGeofence.contains(geofence.id.toLong())) {
                            insideGeofence.remove(geofence.id.toLong())
                            withContext(Dispatchers.Main) {
                                sendGeofenceNotification("Sei uscito dall'area di interesse: ${geofence.name}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MapActivity", "Errore durante il controllo dei geofence: ${e.message}", e)
            }
        }
    }




    private fun sendGeofenceNotification(message: String) {
        // Verifica permesso per le notifiche su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("MapActivity", "Permesso di notifica non concesso.")
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }


    private fun getUserId(): Long {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
