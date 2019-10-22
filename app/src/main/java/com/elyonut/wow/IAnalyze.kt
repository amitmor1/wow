package com.elyonut.wow

import android.location.Location
import java.util.*
import kotlin.collections.ArrayList

interface IAnalyze {
    fun calcRiskStatus(location: Location): Pair<RiskStatus, HashMap<RiskStatus, ArrayList<String>>>
}