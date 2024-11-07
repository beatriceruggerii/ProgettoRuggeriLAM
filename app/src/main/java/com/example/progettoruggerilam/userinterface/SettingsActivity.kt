package com.example.progettoruggerilam.userinterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.repository.ActivityRecordRepository
import com.example.progettoruggerilam.viewmodel.ActivityViewModel
import com.example.progettoruggerilam.viewmodel.ActivityViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SettingsActivity : AppCompatActivity() {

    private lateinit var viewModel: ActivityViewModel
    private lateinit var backBtn: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Inizializza il ViewModel
        val repository = ActivityRecordRepository(AppDatabase.getDatabase(this).activityRecordDao())
        val factory = ActivityViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory).get(ActivityViewModel::class.java)

        // Bottone per cancellare tutti i record dell'utente loggato
        val deleteUserRecordsButton: Button = findViewById(R.id.button_delete_user_records)
        deleteUserRecordsButton.setOnClickListener {
            val userId = getLoggedInUserId()  // Ottieni l'ID dell'utente loggato
            if (userId != -1L) {
                // Usa una coroutine per eseguire l'operazione in background
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.deleteRecordsByUserId(userId)
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "I tuoi record sono stati cancellati.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Errore: ID utente non trovato.", Toast.LENGTH_SHORT).show()
            }
        }


        // Bottone per cancellare definitivamente l'account dell'utente
        val deleteUserButton: Button = findViewById(R.id.button_delete_user)
        deleteUserButton.setOnClickListener {
            showDeleteUserConfirmationDialog()  // Mostra il pop-up di conferma per cancellare l'utente
        }

        // Configura il pulsante di ritorno (backBtn)
        backBtn = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            onBackPressed() // Torna alla schermata precedente
        }
    }

    // Funzione per ottenere l'ID dell'utente loggato dalle SharedPreferences
    private fun getLoggedInUserId(): Long {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1L)  // Restituisce -1 se non è trovato
    }

    // Funzione per mostrare il dialogo di conferma cancellazione utente
    private fun showDeleteUserConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Conferma Cancellazione Account")
        builder.setMessage("Sei sicuro di voler cancellare il tuo account? Questa operazione è irreversibile.")

        // Imposta il bottone di conferma
        builder.setPositiveButton("Sì") { dialog, which ->
            deleteUserAccount()  // Esegui la cancellazione dell'account se confermato
        }

        // Imposta il bottone di annullamento
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()  // Chiudi il dialogo se annullato
        }

        // Mostra il dialogo
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    // Funzione per cancellare l'account utente
    private fun deleteUserAccount() {
        val userId = getLoggedInUserId()  // Ottieni l'ID dell'utente loggato
        if (userId != -1L) {
            // Lancia una coroutine nel lifecycleScope
            lifecycleScope.launch {
                try {
                    // Cancella l'account utente
                    viewModel.deleteUserAccountById(userId)

                    // Rimuovi anche le informazioni dalle SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.clear()  // Cancella tutti i dati salvati
                    editor.apply()

                    // Mostra un messaggio di conferma e torna alla schermata di login
                    Toast.makeText(this@SettingsActivity, "Account cancellato.", Toast.LENGTH_SHORT).show()
                    val loginIntent = Intent(this@SettingsActivity, HomeActivity::class.java)
                    loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(loginIntent)
                    finish()  // Chiude l'activity corrente
                } catch (e: Exception) {
                    // Gestisci eventuali errori
                    Log.e("SettingsActivity", "Errore durante la cancellazione dell'account", e)
                    Toast.makeText(this@SettingsActivity, "Errore durante la cancellazione dell'account.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Errore: Impossibile trovare l'ID dell'utente.", Toast.LENGTH_SHORT).show()
        }
    }
}
