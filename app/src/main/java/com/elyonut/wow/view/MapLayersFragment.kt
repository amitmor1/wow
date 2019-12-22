package com.elyonut.wow.view

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.MapLayersAdapter
import com.elyonut.wow.R
import android.view.Gravity

class MapLayersFragment: DialogFragment() {
    private lateinit var mapLayersRecyclerView: RecyclerView
    private var mapLayersAdapter: MapLayersAdapter? = null
    private var layoutManager: RecyclerView.LayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map_layers, container, false)
        mapLayersRecyclerView = view.findViewById(R.id.map_layers_list)
        mapLayersRecyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,true)
        mapLayersRecyclerView.layoutManager = layoutManager
        mapLayersRecyclerView.itemAnimator = DefaultItemAnimator()
        mapLayersAdapter = MapLayersAdapter(context!!, arrayListOf("a", "b"))
        mapLayersRecyclerView.adapter = mapLayersAdapter


        val window = dialog!!.window
        window!!.setGravity(Gravity.TOP or Gravity.LEFT)
        val params = window.attributes
        params.x = 300
        params.y = 100
        window.attributes = params

        return view
    }
}