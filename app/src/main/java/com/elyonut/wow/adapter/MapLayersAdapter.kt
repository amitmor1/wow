package com.elyonut.wow.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.model.MapLayer
import com.elyonut.wow.view.MapLayersFragment

class MapLayersAdapter(
    var context: Context,
    var mapLayers: ArrayList<MapLayer>,
    onClickHandler: MapLayersFragment.OnClickInterface
) : RecyclerView.Adapter<MapLayersAdapter.MapLayersViewHolder>() {

    var onClickInterface: MapLayersFragment.OnClickInterface = onClickHandler

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
        holder.mapName.text = mapLayers[position].name
        holder.mapTypeImage.setImageResource(mapLayers[position].image)
    }

    inner class MapLayersViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val mapTypeImage: ImageView = view.findViewById(R.id.map_type_image)
        val mapName: TextView = view.findViewById(R.id.map_type_name)

        init{
            mapTypeImage.setOnClickListener(this)
            mapName.setOnClickListener(this)
        }

        override fun onClick(p0: View?) {
            onClickInterface.setClick(p0!!, this.adapterPosition)
        }
    }

}