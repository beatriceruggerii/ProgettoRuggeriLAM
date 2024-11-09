package com.example.progettoruggerilam.userinterface

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.util.GeofenceAdapter
import com.example.progettoruggerilam.util.GeofenceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GeofenceListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var geofenceAdapter: GeofenceAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geofence_list)

        recyclerView = findViewById(R.id.recyclerViewGeofences)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadGeofences()

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Termina l'activity e torna indietro
        }

    }

    private fun loadGeofences() {
        CoroutineScope(Dispatchers.IO).launch {
            db.geofenceDao().getGeofencesByUserId(getUserId()).collect { geofences ->
                withContext(Dispatchers.Main) {
                    geofenceAdapter = GeofenceAdapter(geofences, onDeleteClick = { geofence ->
                        showDeleteConfirmationDialog(geofence)
                    }, onEditClick = { geofence ->
                        showEditGeofenceDialog(geofence)
                    })
                    recyclerView.adapter = geofenceAdapter
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(geofence: GeofenceEntity) {
        AlertDialog.Builder(this)
            .setTitle("Conferma Rimozione")
            .setMessage("Sei sicuro di voler rimuovere il geofence ${geofence.name}?")
            .setPositiveButton("Sì") { _, _ ->
                deleteGeofence(geofence)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteGeofence(geofence: GeofenceEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            db.geofenceDao().deleteGeofence(geofence)
            withContext(Dispatchers.Main) {
                loadGeofences()
                Toast.makeText(this@GeofenceListActivity, "Geofence eliminato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditGeofenceDialog(geofence: GeofenceEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_geofence, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editGeofenceName)
        val radiusInput = dialogView.findViewById<EditText>(R.id.editGeofenceRadius)

        nameInput.setText(geofence.name)
        radiusInput.setText(geofence.radius.toString())

        AlertDialog.Builder(this)
            .setTitle("Modifica Geofence")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val newName = nameInput.text.toString().trim()
                val newRadius = radiusInput.text.toString().toDoubleOrNull()

                if (newName.isBlank() || newRadius == null || newRadius <= 0) {
                    Toast.makeText(this, "Nome o raggio non validi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateGeofence(geofence, newName, newRadius)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun updateGeofence(geofence: GeofenceEntity, newName: String, newRadius: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedGeofence = geofence.copy(name = newName, radius = newRadius.toFloat())
            db.geofenceDao().updateGeofence(updatedGeofence)
            withContext(Dispatchers.Main) {
                loadGeofences()
                Toast.makeText(this@GeofenceListActivity, "Geofence aggiornato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getUserId(): Long {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1L)
    }
}
