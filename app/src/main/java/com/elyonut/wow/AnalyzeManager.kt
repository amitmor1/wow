package com.elyonut.wow

import android.location.Location
import com.mapbox.mapboxsdk.geometry.LatLng

//enum class StatusTypes(val value: Int) {
//    LOW_RISK(R.string.grey_status),
//    MEDIUM_RISK(R.string.orange_status),
//    HIGH_RISK(R.string.red_status)
//}

private const val MY_RISK_RADIUS = 0.3

class AnalyzeManager(private val layerManager: LayerManager) : IAnalyze {
    override fun calcThreatStatus(location: Location): RiskStatus {
        val allFeatures = layerManager.getLayer(Constants.threatLayerId)
        var threatLocation: LatLng
//        var riskStatus = StatusTypes.LOW_RISK
        var riskStatus = RiskStatus.LOW

        run loop@{
            allFeatures?.forEach {
                val threatLat = it.properties?.get("latitude")
                val threatLng = it.properties?.get("longitude")

                if (threatLat != null && threatLng != null) {
                    threatLocation = LatLng(threatLat.asDouble, threatLng.asDouble)
                    val threatRiskRadius = it.properties?.get("radius").let { t -> t?.asDouble }
                    val userLocation = LatLng(location.latitude, location.longitude)

                    val distInKilometers = threatLocation.distanceTo(userLocation) / 1000

                    if (distInKilometers < (MY_RISK_RADIUS + threatRiskRadius!!)) {
                        riskStatus = RiskStatus.MEDIUM

                        if (distInKilometers < threatRiskRadius) {
                            riskStatus = RiskStatus.HIGH
                            return@loop
                        }
                    }
                }
            }
        }

        return riskStatus
    }
}