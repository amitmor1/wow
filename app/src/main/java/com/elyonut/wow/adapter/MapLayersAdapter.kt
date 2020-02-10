package com.elyonut.wow.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R

class MapLayersAdapter(var context: Context, var mapLayers: ArrayList<String>) :
    RecyclerView.Adapter<MapLayersAdapter.MapLayersViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapLayersViewHolder {
        return MapLayersViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.map_layer_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return mapLayers.count()
    }

    override fun onBindViewHolder(holder: MapLayersViewHolder, position: Int) {
        holder.mapDescription.text = mapLayers[position]
    }

    class MapLayersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mapTypeImage: ImageView = view.findViewById(R.id.map_type_image)
        val mapDescription: TextView = view.findViewById(R.id.map_type_description)
    }

}