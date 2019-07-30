package com.elyonut.wow

import android.location.Location
import com.mapbox.geojson.FeatureCollection

enum class StatusEnum(val value: Int) {
    LOW_RISK(R.string.grey_status),
    MEDIUM_RISK(R.string.orange_status),
    HIGH_RISK(R.string.red_status)
}

private const val MY_RISK_RADIUS = 0.3
class CalculationManager(private val tempDB: TempDB): ICalculation {

    override fun calcThreatStatus(location: Location): Int {
//        val allFeatures = FeatureCollection.fromJson(tempDB.getFeatures())
//        var currentFeatureLocation: WowLatLng
//        var riskStatus =  StatusEnum.LOW_RISK
//
//        run loop@{
//            allFeatures.features()?.forEach { it ->
//                val currentLatitude = it.properties()?.get("latitude")
//                val currentLongitude = it.properties()?.get("longitude")
//
//                if ((currentLatitude != null) || (currentLongitude != null)) {
//                    currentFeatureLocation = WowLatLng(currentLatitude!!.asDouble, currentLongitude!!.asDouble)
//                    val featureRiskRadius = it.properties()?.get("radius").let { t -> t?.asDouble }
//
//                    val distSq: Double = kotlin.math.sqrt(
//                        ((location.longitude - currentFeatureLocation.longitude)
//                                * (location.longitude - currentFeatureLocation.longitude))
//                                + ((location.latitude - currentFeatureLocation.latitude)
//                                * (location.latitude - currentFeatureLocation.latitude))
//                    )
//
//                    if (distSq + MY_RISK_RADIUS <= featureRiskRadius!!) {
//                        riskStatus = StatusEnum.HIGH_RISK
//                        return@loop
//                    } else if ((kotlin.math.abs(MY_RISK_RADIUS - featureRiskRadius) <= distSq && distSq <= (MY_RISK_RADIUS + featureRiskRadius))) {
//                        riskStatus = StatusEnum.MEDIUM_RISK
//                    }
//                }
//            }
//        }

        return 1
    }
}