package com.elyonut.wow

import android.location.Location

interface ICalculation {
    fun calcThreatStatus(location: Location): Int
}