package com.elyonut.wow

import com.elyonut.wow.model.FeatureModel
import com.mapbox.mapboxsdk.geometry.LatLng

class RiskData(val currentLocation: LatLng, val riskStatus: RiskStatus, val threatList: List<FeatureModel>) {

}