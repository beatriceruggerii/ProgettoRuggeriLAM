package com.example.progettoruggerilam.userinterface

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.util.User
import com.example.progettoruggerilam.data.dao.UserDao
import com.example.progettoruggerilam.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var userDao: UserDao
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var continuaButton: Button
    private lateinit var alreadyRegistered: TextView  // Aggiunto

    private val CHANNEL_ID = "user_creation_channel"
    private val NOTIFICATION_ID = 1
    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val db = AppDatabase.getDatabase(applicationContext)
        userDao = db.userDao()

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword) // Inizializza campo conferma password
        signUpButton = findViewById(R.id.sigUpBtn)
        continuaButton = findViewById(R.id.continuaButton)

        alreadyRegistered = findViewById(R.id.alreadyRegistered)
        alreadyRegistered.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        createNotificationChannel()

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString() // Ottieni conferma password

            // Controlla che i campi non siano vuoti e che le password corrispondano
            if (username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                Toast.makeText(applicationContext, "Tutti i campi sono obbligatori", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(applicationContext, "Le password non corrispondono", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newUser = User(username = username, password = password)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!userExists(newUser)) {
                        val userId = userDao.insert(newUser)
                        if (userId != -1L) {
                            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putLong("user_id", userId)
                                apply()
                            }
                            runOnUiThread {
                                checkNotificationPermissionAndShow()
                                continuaButton.visibility = View.VISIBLE
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(applicationContext, "Errore durante l'inserimento", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "Utente già esistente", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Errore durante la registrazione", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SignUpActivity", "Error during sign up", e)
                }
            }
        }

        continuaButton.setOnClickListener {
            val intent = Intent(this, HomepageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private suspend fun userExists(user: User): Boolean {
        val allUsers = userDao.getAllUsers()
        return allUsers.any { it.username.equals(user.username, ignoreCase = true) }
    }

    private fun checkNotificationPermissionAndShow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                showUserCreatedNotification()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE_NOTIFICATION_PERMISSION)
            }
        } else {
            showUserCreatedNotification()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserCreatedNotification()
            } else {
                Toast.makeText(this, "Permesso per notifiche non concesso", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUserCreatedNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val intent = Intent(this, HomepageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_user_created)
            .setContentTitle("Utente Creato")
            .setContentText("Clicca continua per accedere alla Homepage")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_continue, "Continua", pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "User Creation Channel"
            val descriptionText = "Notifiche per la creazione di nuovi utenti"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

