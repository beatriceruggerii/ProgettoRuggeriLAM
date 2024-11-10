package com.example.progettoruggerilam.userinterface


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.util.GeofenceEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import androidx.core.graphics.drawable.toBitmap
import com.example.progettoruggerilam.util.MarkerDialogFragment


class MapActivity : AppCompatActivity() , MarkerDialogFragment.MarkerDialogListener {

    private lateinit var mapView: MapView
    private lateinit var txtNotification: TextView
    private lateinit var backBtn: FloatingActionButton
    private lateinit var locationManager: LocationManager
    private var userLocationMarker: Marker? = null
    private val db by lazy { AppDatabase.getDatabase(this) } // Database Room
    private var currentUserId: Long = -1 // ID dell'utente corrente
    private val insideGeofence: MutableSet<Long> = mutableSetOf()
    private var currentZoomLevel: Double = 15.0
    private val CHANNEL_ID = "geofence_notifications"
    private val NOTIFICATION_ID = 1
    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 101
        private const val CHANNEL_ID = "geofence_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))

        mapView = findViewById(R.id.map)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(5.5)
        mapView.controller.setCenter(GeoPoint(41.8719, 12.5674))

        txtNotification = findViewById(R.id.txtNotification)

        backBtn = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }

        currentUserId = getUserId() // Ottieni l'ID dell'utente corrente

        val overlayTouchListener = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val geoPoint = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                showMarkerDialog(geoPoint)
                return true
            }
        }
        mapView.overlays.add(overlayTouchListener)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }

        // Mostra la posizione corrente e i marker dell'utente corrente
        setupMap()
        showCurrentLocation()
        loadUserMarkers()
        createNotificationChannel()


    }

    private fun setupMap() {
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
    }

    override fun onMarkerAdded(name: String, radius: Double, geoPoint: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            saveMarkerToDatabase(name, radius, geoPoint)
            withContext(Dispatchers.Main) {
                addMarker(geoPoint, name, radius)
                showNotification("Marker aggiunto in posizione: ${geoPoint.latitude}, ${geoPoint.longitude}")
            }
        }
    }

    private fun showMarkerDialog(geoPoint: GeoPoint) {
        val dialog = MarkerDialogFragment.newInstance(geoPoint)
        dialog.show(supportFragmentManager, "MarkerDialogFragment")
    }

    private fun addMarker(geoPoint: GeoPoint, name: String, radius: Double) {
        val marker = Marker(mapView)
        marker.position = geoPoint
        marker.title = "$name\nRaggio: ${radius.toInt()} metri" // A capo per il raggio
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)

        val circle = Polygon()
        circle.points = Polygon.pointsAsCircle(geoPoint, radius)
        circle.fillPaint.color = 0x4000FF00
        circle.outlinePaint.color = 0xFF00FF00.toInt()
        circle.outlinePaint.strokeWidth = 2.0f

        mapView.overlays.add(circle)
        mapView.invalidate()
    }

    private fun saveMarkerToDatabase(name: String, radius: Double, geoPoint: GeoPoint) {
        val markerEntity = GeofenceEntity(
            name = name,
            radius = radius.toFloat(),
            latitude = geoPoint.latitude,
            longitude = geoPoint.longitude,
            userId = currentUserId,
            createdAt = System.currentTimeMillis()
        )

        CoroutineScope(Dispatchers.IO).launch {
            db.geofenceDao().insertGeofence(markerEntity)
        }
    }

    private fun loadUserMarkers() {
        CoroutineScope(Dispatchers.IO).launch {
            val userMarkers = db.geofenceDao().getGeofencesByUserId(currentUserId)
            withContext(Dispatchers.Main) {
                userMarkers.collect { markers ->
                    markers.forEach { markerEntity ->
                        val geoPoint = GeoPoint(markerEntity.latitude, markerEntity.longitude)
                        addMarker(geoPoint, markerEntity.name, markerEntity.radius.toDouble())
                    }
                }
            }
        }
    }

    private fun getUserId(): Long {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getLong("user_id", -1L)
    }

    private fun showNotification(message: String) {
        txtNotification.text = message
        txtNotification.visibility = View.VISIBLE
        txtNotification.postDelayed({
            txtNotification.visibility = View.GONE
        }, 3000)
    }

    private fun showCurrentLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Prova a ottenere l'ultima posizione conosciuta subito all'avvio
            val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            lastKnownLocation?.let { location ->
                val currentLocation = GeoPoint(location.latitude, location.longitude)
                updateLocationMarker(currentLocation)
                mapView.controller.setCenter(currentLocation) // Centra sulla posizione corrente
                mapView.controller.setZoom(15.0)
            }

            // Ascolta gli aggiornamenti della posizione
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 10f, locationListener)
        }


    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val currentLocation = GeoPoint(location.latitude, location.longitude)
            updateLocationMarker(currentLocation)
            checkGeofenceEntryExit(currentLocation)
        }
    }

    private fun updateLocationMarker(geoPoint: GeoPoint) {
        if (!::mapView.isInitialized || mapView == null) {
            Log.e("MapActivity", "MapView non è stato inizializzato correttamente.")
            return
        }

        try {
            // Rimuovi il marker precedente, se esistente
            userLocationMarker?.let { mapView.overlays.remove(it) }

            val drawable = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_marker_red)
            val bitmap = drawable?.toBitmap(50, 50) // Ridimensiona l'icona a 50x50 pixel

            // Crea un nuovo marker con l'icona ridimensionata
            userLocationMarker = Marker(mapView).apply {
                position = geoPoint
                title = "La tua posizione"
                icon = BitmapDrawable(resources, bitmap)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }

            mapView.overlays.add(userLocationMarker)

            // Verifica se il livello di zoom corrente è diverso da quello impostato e centra senza cambiare zoom
            if (mapView.zoomLevelDouble != currentZoomLevel) {
                mapView.controller.setZoom(currentZoomLevel)
            }
            mapView.controller.setCenter(geoPoint)

            mapView.invalidate()
        } catch (e: NullPointerException) {
            Log.e("MapActivity", "Errore nell'aggiornamento del marker: ${e.message}")
        }
    }

    private fun checkGeofenceEntryExit(currentLocation: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            db.geofenceDao().getGeofencesByUserId(currentUserId).collect { geofences ->
                geofences.forEach { geofence ->
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
        }
    }

    private fun sendGeofenceNotification(message: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
            return
        }

        // Crea e mostra la notifica
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendGeofenceNotification("Sei entrato nell'area di interesse!") // Oppure il messaggio appropriato
            } else {
                showNotification("Permesso per le notifiche non concesso")
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Geofence Alerts"
            val descriptionText = "Notifiche per le aree di interesse"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_map_activity, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_center_location -> {
                centerMapOnCurrentLocation()
                true
            }
            R.id.action_delete_markers -> {
                deleteAllMarkers()
                true
            }

            R.id.action_view_geofences -> {
                startActivity(Intent(this, GeofenceListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteAllMarkers() {
        val overlaysToRemove = mapView.overlays.filter { overlay ->
            overlay is Marker && overlay != userLocationMarker
        }

        mapView.overlays.removeAll(overlaysToRemove)
        mapView.invalidate()

        CoroutineScope(Dispatchers.IO).launch {
            db.geofenceDao().deleteAllGeofences()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MapActivity, "Tutti i marker eliminati tranne la posizione corrente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeMarkerFromMap(latitude: Double, longitude: Double) {
        if (!::mapView.isInitialized) {
            Log.e("MapActivity", "MapView non è stato inizializzato correttamente.")
            return
        }

        val overlayToRemove = mapView.overlays.find { overlay ->
            overlay is Marker && overlay.position.latitude == latitude && overlay.position.longitude == longitude
        }

        if (overlayToRemove != null) {
            mapView.overlays.remove(overlayToRemove)
            Log.d("MapActivity", "Rimozione marker con lat: $latitude, long: $longitude")
            mapView.postInvalidate()
        } else {
            Log.e("MapActivity", "Marker non trovato con lat: $latitude, long: $longitude")
        }
    }

    private fun centerMapOnCurrentLocation() {
        // Centra la mappa sulla posizione corrente
        userLocationMarker?.let {
            mapView.controller.animateTo(it.position)
        }
    }

    private val geofenceRemovedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)

            Handler(Looper.getMainLooper()).post {
                removeMarkerFromMap(latitude, longitude)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter("com.example.progettoruggerilam.GEOFENCE_REMOVED")
        registerReceiver(geofenceRemovedReceiver, intentFilter)
        Log.d("MapActivity", "GeofenceRemovedReceiver registrato")
    }


}