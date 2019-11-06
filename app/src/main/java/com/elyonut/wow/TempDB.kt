package com.elyonut.wow

import android.content.Context
import com.elyonut.wow.model.FeatureModel
import com.elyonut.wow.model.LayerModel
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class TempDB(var context: Context) {
    fun getFeatures(): ArrayList<LayerModel>? {

        // threat features
        val gson = GsonBuilder().create()
        val buffer = BufferedReader(InputStreamReader(context.assets.open("constructionFeatures2.geojson")))
//        var buffer = BufferedReader(InputStreamReader(context.assets.open("constructionFeatures.geojson")))
        val features = gson.fromJson(buffer, Array<FeatureModel>::class.java)
        val layerModel = LayerModel(Constants.THREAT_LAYER_ID, "threat", features.toList())
        val layersList = ArrayList<LayerModel>()
        layersList.add(layerModel)

        //trains features
//        buffer = BufferedReader(InputStreamReader(context.assets.open("trains.geojson")))
//        features = gson.fromJson(buffer, Array<FeatureModel>::class.java)
//        layerModel = LayerModel(Constants.trainsLayerId, "trains", features.toList())
//        layersList.add(layerModel)

        return layersList
    }
}