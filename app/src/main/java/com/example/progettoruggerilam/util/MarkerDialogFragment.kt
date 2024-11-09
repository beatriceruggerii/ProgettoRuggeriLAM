package com.example.progettoruggerilam.util

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.progettoruggerilam.R
import com.example.progettoruggerilam.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class MarkerDialogFragment : DialogFragment() {

    interface MarkerDialogListener {
        fun onMarkerAdded(name: String, radius: Double, geoPoint: GeoPoint)
    }

    private var listener: MarkerDialogListener? = null
    private lateinit var geoPoint: GeoPoint
    private val db by lazy { AppDatabase.getDatabase(requireContext()) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? MarkerDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        geoPoint = arguments?.getParcelable("geoPoint") ?: GeoPoint(0.0, 0.0)

        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_geofence, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.geofenceName)
        val radiusInput = dialogView.findViewById<EditText>(R.id.geofenceRadius)

        return AlertDialog.Builder(requireContext())
            .setTitle("Aggiungi un Marker")
            .setView(dialogView)
            .setPositiveButton("Salva") { _, _ ->
                val name = nameInput.text.toString().trim()
                val radius = radiusInput.text.toString().toDoubleOrNull()

                if (name.isBlank()) {
                    Toast.makeText(context, "Il nome del marker è obbligatorio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (radius == null || radius <= 0) {
                    Toast.makeText(context, "Inserisci un raggio valido maggiore di zero", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Controlla se esiste già un marker con lo stesso nome
                CoroutineScope(Dispatchers.IO).launch {
                    val existingMarker = db.geofenceDao().getGeofenceByName(name)
                    withContext(Dispatchers.Main) {
                        if (existingMarker != null) {
                            Toast.makeText(context, "Esiste già un marker con questo nome", Toast.LENGTH_SHORT).show()
                        } else {
                            listener?.onMarkerAdded(name, radius, geoPoint)
                        }
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .create()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance(geoPoint: GeoPoint): MarkerDialogFragment {
            val fragment = MarkerDialogFragment()
            val args = Bundle().apply {
                putParcelable("geoPoint", geoPoint)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
