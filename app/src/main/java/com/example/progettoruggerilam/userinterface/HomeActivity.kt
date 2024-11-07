package com.example.progettoruggerilam.userinterface

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.receiver.ReminderReceiver
import com.example.progettoruggerilam.service.ActivityReminderService
import com.example.progettoruggerilam.service.GeofenceForegroundService
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        checkNotificationPermission()
        // Programma notifiche periodiche (eventualmente aumentare l'intervallo)
        scheduleActivityReminder(this)
        startActivityMonitoringService()



        val usernameEditText = findViewById<EditText>(R.id.username)
        val passwordEditText = findViewById<EditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginbtn)
        val signUpButton = findViewById<Button>(R.id.sigUpBtn)

        val database = AppDatabase.getDatabase(this)
        val userDao = database.userDao()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this@HomeActivity, "Inserisci username e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val user = userDao.getUser(username, password)
                if (user != null) {
                    saveUserId(user.userId)
                    Toast.makeText(this@HomeActivity, "Accesso riuscito", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@HomeActivity, HomepageActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@HomeActivity, "Credenziali non valide", Toast.LENGTH_SHORT).show()
                }
            }
        }

        signUpButton.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        val serviceIntent = Intent(this, GeofenceForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)



    }

    private fun saveUserId(userId: Long) {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("user_id", userId).apply()
    }

    fun scheduleActivityReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.progettoruggerilam.ACTION_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Imposta il reminder per attivarsi ogni minuto
        val intervalMillis = TimeUnit.MINUTES.toMillis(5)
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }

    private fun startActivityMonitoringService() {
        val serviceIntent = Intent(this, ActivityReminderService::class.java)
        startService(serviceIntent)
    }


}
