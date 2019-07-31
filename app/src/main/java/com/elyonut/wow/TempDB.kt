package com.elyonut.wow

import android.content.Context
import java.io.InputStream

class TempDB(var context: Context) {
     fun getFeatures(): String {
        val stream: InputStream = context.assets.open("check.geojson")
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        return String(buffer, charset("UTF-8"))
    }
}