package com.example.progettoruggerilam.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.progettoruggerilam.R

class GeofenceAdapter(
    private val geofences: List<GeofenceEntity>,
    private val onDeleteClick: (GeofenceEntity) -> Unit,
    private val onEditClick: (GeofenceEntity) -> Unit
) : RecyclerView.Adapter<GeofenceAdapter.GeofenceViewHolder>() {

    inner class GeofenceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tvGeofenceName)
        val radiusTextView: TextView = view.findViewById(R.id.tvGeofenceRadius)
        val deleteButton: Button = view.findViewById(R.id.btnDeleteGeofence)
        val editButton: Button = view.findViewById(R.id.btnEditGeofence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeofenceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_geofence, parent, false)
        return GeofenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: GeofenceViewHolder, position: Int) {
        val geofence = geofences[position]
        holder.nameTextView.text = geofence.name
        holder.radiusTextView.text = "Raggio: ${geofence.radius} m"

        holder.deleteButton.setOnClickListener { onDeleteClick(geofence) }
        holder.editButton.setOnClickListener { onEditClick(geofence) }
    }

    override fun getItemCount() = geofences.size
}
