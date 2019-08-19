package com.elyonut.wow

import android.content.Context
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LayerModel
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class TempDB(var context: Context) {
    fun getFeatures(): ArrayList<LayerModel>? {
        val gson = GsonBuilder().create()
        val buffer = BufferedReader(InputStreamReader(context.assets.open("featurescopy2.geojson")))
        val features =   gson.fromJson(buffer, Array<FeatureModel>::class.java)
        val layerModel = LayerModel("buildingrisk","threat", features.toList())
        val layerList = ArrayList<LayerModel>()
        layerList.add(layerModel)
        return layerList
    }
}