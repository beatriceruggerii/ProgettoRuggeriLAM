package com.example.progettoruggerilam.userinterface

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.progettoruggerilam.viewmodel.ActivityViewModel
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.repository.ActivityRecordRepository
import com.example.progettoruggerilam.data.database.AppDatabase
import com.example.progettoruggerilam.service.ActivityReminderService
import com.example.progettoruggerilam.viewmodel.ActivityViewModelFactory
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChartsActivity : AppCompatActivity() {

    private lateinit var viewModel: ActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        // Inizializza il repository e il ViewModel
        val repository = ActivityRecordRepository(AppDatabase.getDatabase(this).activityRecordDao())
        val factory = ActivityViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory).get(ActivityViewModel::class.java)

        // Inizializza i grafici e il bottone per tornare indietro
        val pieChart = findViewById<PieChart>(R.id.pieChart)
        val lineChart = findViewById<LineChart>(R.id.lineChart)
        val backButton = findViewById<FloatingActionButton>(R.id.backBtn)



        backButton.setOnClickListener {
            finish() // Torna alla Homepage
        }

        // Mappa dei colori per ciascun tipo di attività
        val activityColors = mapOf(
            "Sconosciuto" to ColorTemplate.rgb("#FF5722"),  // Arancione per "Sconosciuto"
            "Camminare" to ColorTemplate.rgb("#4CAF50"),    // Verde per "Camminare"
            "Fermo" to ColorTemplate.rgb("#2196F3"),        // Blu per "Fermo"
            "Guidare" to ColorTemplate.rgb("#9C27B0")       // Viola per "Guidare"
        )

        // Recupera l'userId dai SharedPreferences
        val userId = getUserId()

        if (userId == -1L) {
            // Gestisci l'errore, ad esempio mostrando un messaggio all'utente
            return
        }

        // Osserva i record dell'utente per tutto il mese
        viewModel.getRecordsByUserForMonth(userId).observe(this, Observer { records ->
            // Raggruppa i record per tipo di attività e conta quante volte ciascuna attività è stata registrata
            val activityCount = records.groupBy { it.activityName }
                .mapValues { entry -> entry.value.size }

            // Crea le voci per il grafico a torta, includendo il numero di volte che ciascuna attività è stata registrata
            val pieEntries = activityCount.map { (activityName, count) ->
                PieEntry(
                    count.toFloat(),
                    "$activityName ($count volte)"  // Etichetta che include l'attività e il conteggio
                )
            }
            val pieDataSet = PieDataSet(pieEntries, " ")

            // Imposta i colori per ciascun tipo di attività
            pieDataSet.colors = pieEntries.map { entry ->
                activityColors[entry.label.split(" ")[0]] ?: ColorTemplate.rgb("#000000")
            }

            val pieData = PieData(pieDataSet)
            pieChart.data = pieData
            pieChart.invalidate()  // Aggiorna il grafico a torta

            // Creazione delle voci per il grafico a linee, includendo solo l'attività "Camminare"
            val lineEntries = records
                .filter { it.activityName == "Camminare" }  // Include solo l'attività "Camminare"
                .mapIndexed { index, record ->
                    Entry(
                        index.toFloat(),
                        record.steps.toFloat()
                    )
                }

            val lineDataSet = LineDataSet(lineEntries, "Andamento Camminata")
            lineDataSet.colors = ColorTemplate.COLORFUL_COLORS.asList()

            val lineData = LineData(lineDataSet)
            lineChart.data = lineData
            lineChart.invalidate()  // Aggiorna il grafico a linee
        })
    }

    // Funzione per ottenere l'ID utente dai SharedPreferences
    private fun getUserId(): Long {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("user_id", -1L)  // Restituisce -1 se non trovato
    }


}
