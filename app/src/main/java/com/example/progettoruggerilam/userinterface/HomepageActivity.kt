package com.example.progettoruggerilam.userinterface


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

class HomepageActivity : AppCompatActivity() {

    private lateinit var viewModel: ActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepage)

        // Inizializza il ViewModel
        val repository = ActivityRecordRepository(AppDatabase.getDatabase(this).activityRecordDao())
        val factory = ActivityViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory).get(ActivityViewModel::class.java)

        // Imposta i listener per la navigazione
        findViewById<Button>(R.id.button_charts).setOnClickListener { navigateToActivity(ChartsActivity::class.java) }
        findViewById<Button>(R.id.button_main).setOnClickListener { navigateToActivity(PhysicalActivityMonitorActivity::class.java) }
        findViewById<Button>(R.id.button_history).setOnClickListener { navigateToActivity(HistoryActivity::class.java) }
        findViewById<Button>(R.id.button_map).setOnClickListener { navigateToActivity(MapActivity::class.java) }

        // Bottone per il Sign Out
        findViewById<Button>(R.id.button_signout).setOnClickListener { showSignOutDialog() }
    }

    private fun navigateToActivity(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setMessage("Sei sicuro di uscire dall'app?")
            .setPositiveButton("Sì") { dialog, _ ->
                dialog.dismiss()
                finishAffinity() // Chiude tutte le attività e esce dall'app
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_homepage, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Apri l'attività Impostazioni
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                // Mostra un dialogo con le informazioni
                Toast.makeText(this, "Informazioni sull'app", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_logout -> {
                // Esegui il logout e torna alla schermata di login
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
