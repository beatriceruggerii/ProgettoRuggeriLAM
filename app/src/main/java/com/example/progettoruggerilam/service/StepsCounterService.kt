package com.example.progettoruggerilam.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log

class StepsCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepsAtStart = -1
    private var totalSteps = 0
    private var currentSteps = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("StepsCounterService", "Service creato.")

        // Inizializza il SensorManager e il sensore di conteggio passi
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            Log.e("StepsCounterService", "Sensore di conteggio passi non disponibile!")
            stopSelf()
        } else {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            Log.d("StepsCounterService", "Sensore di conteggio passi registrato.")
        }

        // Recupera i passi salvati all'avvio
        val sharedPreferences = getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        stepsAtStart = sharedPreferences.getInt("steps_at_start", -1)
        currentSteps = sharedPreferences.getInt("steps", 0)
        Log.d("StepsCounterService", "Passi recuperati all'avvio: $currentSteps")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("StepsCounterService", "onStartCommand ricevuto.")
        if (stepSensor != null) {
            sensorManager.unregisterListener(this)
            val registered = sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            if (registered) {
                Log.d("StepsCounterService", "Sensore registrato correttamente.")
            } else {
                Log.e("StepsCounterService", "Registrazione del sensore fallita.")
            }
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            // Calcolo dei passi
            if (stepsAtStart == -1) {
                stepsAtStart = event.values[0].toInt()
                salvaInSharedPreferences("steps_at_start", stepsAtStart)
            }
            totalSteps = event.values[0].toInt()
            currentSteps = totalSteps - stepsAtStart

            salvaInSharedPreferences("steps", currentSteps)
            salvaInSharedPreferences("last_update_time", System.currentTimeMillis())

            Log.d("StepsCounterService", "Passi correnti aggiornati: $currentSteps")

            // Crea l’intent per inviare il broadcast
            val stepIntent = Intent("com.example.progettoruggerilam.STEP_UPDATE")
            stepIntent.putExtra("steps", currentSteps)

            // Invia il broadcast localmente
            LocalBroadcastManager.getInstance(this).sendBroadcast(stepIntent)
        }
    }

    private fun salvaInSharedPreferences(key: String, valore: Int) {
        val sharedPreferences = getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt(key, valore)
            apply()
        }
        Log.d("StepsCounterService", "Salvato $key in SharedPreferences: $valore")
    }

    private fun salvaInSharedPreferences(key: String, valore: Long) {
        val sharedPreferences = getSharedPreferences("steps_prefs", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong(key, valore)
            apply()
        }
        Log.d("StepsCounterService", "Salvato $key in SharedPreferences: $valore")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)

        // Reset del conteggio dei passi
        stepsAtStart = totalSteps
        currentSteps = 0
        salvaInSharedPreferences("steps_at_start", stepsAtStart)
        salvaInSharedPreferences("steps", currentSteps)

        Log.d("StepsCounterService", "Service distrutto, conteggio passi resettato.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
