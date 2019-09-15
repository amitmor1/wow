package com.elyonut.wow

import com.elyonut.wow.model.PolygonModel
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken

abstract class GeoJsonAdapterFactory() : TypeAdapterFactory {

    //    public static create(): TypeAdapterFactory {}
//    class GeoJsonAdapterFactoryIml : GeoJsonAdapterFactory() {
//        override fun <T : Any?> create(gson: Gson?, type: TypeToken<T>?): TypeAdapter<T> {
//            val rawType = type?.rawType
//            if (PolygonModel::class.java.isAssignableFrom(rawType))
//
//        }
//
//    }
}