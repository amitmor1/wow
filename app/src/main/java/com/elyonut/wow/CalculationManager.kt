package com.elyonut.wow

import android.location.Location
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLng

enum class StatusEnum(val value: Int) {
    LOW_RISK(R.string.grey_status),
    MEDIUM_RISK(R.string.orange_status),
    HIGH_RISK(R.string.red_status)
}

private const val MY_RISK_RADIUS = 0.3
class CalculationManager(private val bl: TempDB): ICalculation {

     override fun calcThreatStatus(location: Location): Int {
        val allFeatures = FeatureCollection.fromJson(bl.getFeatures())
        var threatLocation: LatLng
        var riskStatus =  StatusEnum.LOW_RISK

        run loop@{
            allFeatures.features()?.forEach { it ->
                val threatLat = it.properties()?.get("latitude")
                val threatLng = it.properties()?.get("longitude")

                if (threatLat != null && threatLng != null) {
                    threatLocation = LatLng(threatLat.asDouble, threatLng.asDouble)
                    val threatRiskRadius = it.properties()?.get("radius").let { t -> t?.asDouble }
                    val  userLocation = LatLng(location.latitude, location.longitude)

                    val distInKilometers = threatLocation.distanceTo(userLocation) / 1000

                    if(distInKilometers < (MY_RISK_RADIUS + threatRiskRadius!!)){
                        riskStatus =  StatusEnum.MEDIUM_RISK

                        if(distInKilometers < threatRiskRadius){
                            riskStatus = StatusEnum.HIGH_RISK
                            return@loop
                        }
                    }
                }
            }
        }

        return riskStatus.value
    }
}