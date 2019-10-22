package com.elyonut.wow

import android.location.Location

interface IAnalyze {
    fun calcThreatStatus(location: Location): Pair<RiskStatus, String?>
}