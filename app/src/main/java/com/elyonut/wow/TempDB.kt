package com.elyonut.wow

import android.content.Context
import com.elyonut.wow.model.FeatureModel
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class TempDB(var context: Context) {
    fun getFeatures(): Array<FeatureModel> {
        val gson = GsonBuilder().create()
        val buffer = BufferedReader(InputStreamReader(context.assets.open("featurescopy2.geojson")))
        return gson.fromJson(buffer, Array<FeatureModel>::class.java)
    }
}