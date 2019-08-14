package com.elyonut.wow.view

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.elyonut.wow.R
import com.elyonut.wow.model.LayerModel

class LayerMenuAdapter(private val layersDataSet: Array<LayerModel>) :
    RecyclerView.Adapter<LayerMenuAdapter.LayerViewHolder>() {
    private val idMap = SparseArray<String>()

    class LayerViewHolder(checkBoxView: View) : RecyclerView.ViewHolder(checkBoxView) {
        val checkBox: CheckBox = checkBoxView.findViewById(R.id.layerCheckbox)

        init {
            checkBox.setOnClickListener {
                if (checkBox.isChecked) {

                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val checkBox = LayoutInflater.from(parent.context).inflate(R.layout.checkbox_row_item, parent, false)
        return LayerViewHolder(checkBox)
    }

    override fun onBindViewHolder(layerViewHolder: LayerViewHolder, position: Int) {
        idMap.append(position, layersDataSet[position].id)
        layerViewHolder.checkBox.text = layersDataSet[position].name
    }

    override fun getItemCount() = layersDataSet.size

}