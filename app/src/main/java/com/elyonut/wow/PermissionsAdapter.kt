package com.elyonut.wow

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.mapbox.android.core.permissions.PermissionsManager

class PermissionsAdapter(private var context: Context) : IPermissions {
    private var hasPermissions = false

    override fun checkLocationPermissions(): Boolean {
        if ((PermissionsManager.areLocationPermissionsGranted(context))
            && (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            hasPermissions = true
        } else {

        }

        return hasPermissions
    }
}