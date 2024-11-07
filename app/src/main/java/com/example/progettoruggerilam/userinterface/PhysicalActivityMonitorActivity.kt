package com.example.progettoruggerilam.userinterface

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.repository.ActivityRecordRepository
import com.example.progettoruggerilam.service.StepsCounterService
import com.example.progettoruggerilam.viewmodel.ActivityViewModel
import com.example.progettoruggerilam.viewmodel.ActivityViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PhysicalActivityMonitorActivity : AppCompatActivity() {

    private val viewModel: ActivityViewModel by viewModels {
        val repository = ActivityRecordRepository(AppDatabase.getDatabase(this).activityRecordDao())
        ActivityViewModelFactory(application, repository)
    }
    private var isTracking = false
    private var currentActivityName: String? = null
    private lateinit var startTrackingButton: Button
    private lateinit var stopTrackingButton: Button
    private lateinit var choiceRadioGroup: RadioGroup
    private var startTime: Long = 0L

    private val stepReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (currentActivityName == "Camminare" && intent != null && intent.hasExtra("steps")) {
                val steps = intent.getIntExtra("steps", 0)
                Log.d("PhysicalActivityMonitor", "Passi ricevuti dal BroadcastReceiver: $steps")
                findViewById<TextView>(R.id.activityStatus).text =
                    getString(R.string.steps_display, steps)
            } else {
                Log.e("PhysicalActivityMonitor", "Intent vuoto o senza dati per 'steps' o attività diversa da Camminare")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.example.progettoruggerilam.STEP_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(stepReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stepReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_physicalactivitymonitoractivity)

        startTrackingButton = findViewById(R.id.startTrackingButton)
        stopTrackingButton = findViewById(R.id.stopTrackingButton)
        startTrackingButton.isEnabled = false

        val activityStatusTextView = findViewById<TextView>(R.id.activityStatus)
        choiceRadioGroup = findViewById(R.id.choiceRadioGroup)

        choiceRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            currentActivityName = when (checkedId) {
                R.id.radio_still -> "Fermo"
                R.id.radio_drive -> "Guidare"
                R.id.radio_walk -> "Camminare"
                else -> null
            }

            startTrackingButton.isEnabled = currentActivityName != null

            // Aggiorna il messaggio visualizzato in base all'attività selezionata e gestisce il conteggio dei passi
            when (currentActivityName) {
                "Camminare" -> {
                    activityStatusTextView.text = getString(R.string.steps_display, 0) // Mostra i passi con conteggio iniziale a 0
                    avviaConteggioPassi() // Inizia a contare i passi
                }
                "Guidare" -> {
                    activityStatusTextView.text = "Stai guidando"
                    interrompiConteggioPassi() // Ferma il conteggio dei passi
                }
                "Fermo" -> {
                    activityStatusTextView.text = "Sei fermo"
                    interrompiConteggioPassi() // Ferma il conteggio dei passi
                }
                else -> {
                    activityStatusTextView.text = "" // Nessun messaggio se non è selezionata un'attività valida
                    interrompiConteggioPassi()
                }
            }
        }

        startTrackingButton.setOnClickListener { startTracking() }
        stopTrackingButton.setOnClickListener { stopTracking(currentActivityName ?: "Sconosciuto") }

        val backBtn: FloatingActionButton = findViewById(R.id.backBtn)
        backBtn.setOnClickListener { finish() }
    }

    private fun startTracking() {
        startTime = System.currentTimeMillis()
        isTracking = true
        startTrackingButton.isEnabled = false
        stopTrackingButton.isEnabled = true
        choiceRadioGroup.isEnabled = false // Disabilita il RadioGroup
        avviaConteggioPassi()

        findViewById<RadioButton>(R.id.radio_still).isEnabled = false
        findViewById<RadioButton>(R.id.radio_drive).isEnabled = false
        findViewById<RadioButton>(R.id.radio_walk).isEnabled = false


        Toast.makeText(this, "Tracking dell'attività \"$currentActivityName\" incominciato.", Toast.LENGTH_SHORT).show()
    }

    private fun stopTracking(activityName: String) {
        if (!isTracking) return
        val endTime = System.currentTimeMillis()
        val passi = getPassiReali().first.takeIf { activityName == "Camminare" }
        salvaAttivitaNelDatabase(activityName, passi, startTime, endTime)
        isTracking = false
        startTrackingButton.isEnabled = true
        stopTrackingButton.isEnabled = false
        choiceRadioGroup.isEnabled = true // Riabilita il RadioGroup
        interrompiConteggioPassi()

        findViewById<RadioButton>(R.id.radio_still).isEnabled = true
        findViewById<RadioButton>(R.id.radio_drive).isEnabled = true
        findViewById<RadioButton>(R.id.radio_walk).isEnabled = true


        // Reset del conteggio passi
        val sharedPreferences = getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt("steps", 0)
            apply()
        }

        Toast.makeText(this, "Tracking dell'attività \"$activityName\" interrotto.", Toast.LENGTH_SHORT).show()
    }

    private fun salvaAttivitaNelDatabase(activityName: String, steps: Int?, startTime: Long, endTime: Long) {
        val passiEffettivi = if (activityName == "Camminare") steps else null
        viewModel.insert(
            activityName = activityName,
            steps = passiEffettivi ?: 0,
            timestamp = startTime,
            endTime = endTime
        )
    }

    private fun avviaConteggioPassi() {
        val intent = Intent(this, StepsCounterService::class.java)
        startService(intent)
        Log.d("PhysicalActivityMonitor", "Conteggio passi attivato.")
    }

    private fun interrompiConteggioPassi() {
        val intent = Intent(this, StepsCounterService::class.java)
        stopService(intent)
        Log.d("PhysicalActivityMonitor", "Conteggio passi disattivato.")
    }

    private fun getPassiReali(): Pair<Int, Long> {
        val sharedPreferences = getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        val passi = sharedPreferences.getInt("steps", 0)
        val lastUpdateTime = sharedPreferences.getLong("last_update_time", 0L)
        return Pair(passi, lastUpdateTime)
    }
}

