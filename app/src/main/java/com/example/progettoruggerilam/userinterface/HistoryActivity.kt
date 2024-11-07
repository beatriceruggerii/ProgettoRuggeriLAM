package com.example.progettoruggerilam.userinterface

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.CalendarView
import android.widget.ListView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.repository.ActivityRecordRepository
import com.example.progettoruggerilam.viewmodel.ActivityViewModel
import com.example.progettoruggerilam.viewmodel.ActivityViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var activityViewModel: ActivityViewModel
    private var isSortByDurationEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Inizializza il repository e il ViewModel
        val activityDao = AppDatabase.getDatabase(applicationContext).activityRecordDao()
        val activityRepository = ActivityRecordRepository(activityDao)
        val factory = ActivityViewModelFactory(application, activityRepository)
        activityViewModel = ViewModelProvider(this, factory).get(ActivityViewModel::class.java)

        val backButton: FloatingActionButton = findViewById(R.id.backBtn)
        backButton.setOnClickListener { finish() }

        val calendarView: CalendarView = findViewById(R.id.calendarView)
        val activityListView: ListView = findViewById(R.id.activityListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        activityListView.adapter = adapter

        // SwitchCompat per il filtro
        val switchFilter: SwitchCompat = findViewById(R.id.switchCompat)
        switchFilter.setOnCheckedChangeListener { _, isChecked ->
            isSortByDurationEnabled = isChecked
            // Aggiorna i dati per la data selezionata corrente
            val selectedDate = dateFormat.format(calendarView.date)
            checkDataForSelectedDate(selectedDate, adapter, getUserId())
        }

        // Controlla l'ID utente prima di procedere
        val userId = getUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Errore: ID utente non trovato.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Imposta l'osservatore per la data selezionata sul calendario
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = dateFormat.format(GregorianCalendar(year, month, dayOfMonth).time)
            checkDataForSelectedDate(selectedDate, adapter, userId)
        }
    }

    private fun checkDataForSelectedDate(selectedDate: String, adapter: ArrayAdapter<String>, userId: Long) {
        activityViewModel.getActivitiesByDateAndUser(selectedDate, userId).observe(this, Observer { records ->
            if (records.isNullOrEmpty()) {
                adapter.clear()
                adapter.add("Nessuna attività registrata per questa data")
            } else {
                // Ordina i record in base alla durata se il filtro è attivo
                val sortedRecords = if (isSortByDurationEnabled) {
                    records.sortedByDescending { (it.endTime ?: 0) - it.timestamp }
                } else {
                    records
                }

                // Crea la lista delle attività per la visualizzazione
                val activitiesForDate = sortedRecords.map { record ->
                    val duration = (record.endTime ?: record.timestamp) - record.timestamp
                    val passiInfo = if (record.activityName == "Camminare") "Passi: ${record.steps}" else ""
                    "Attività: ${record.activityName}, Durata: ${formatDuration(duration)}, Inizio: ${formatTime(record.timestamp)}, Fine: ${formatTime(record.endTime ?: 0)} $passiInfo"
                }

                adapter.clear()
                adapter.addAll(activitiesForDate)
            }
            adapter.notifyDataSetChanged()
        })
    }

    private fun formatTime(timeInMillis: Long): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timeInMillis))
    }

    private fun formatDuration(durationInMillis: Long): String {
        val seconds = durationInMillis / 1000 % 60
        val minutes = durationInMillis / (1000 * 60) % 60
        val hours = durationInMillis / (1000 * 60 * 60)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun getUserId(): Long {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1L)
    }
}
