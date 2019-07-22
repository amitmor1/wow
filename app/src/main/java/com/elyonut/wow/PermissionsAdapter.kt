package com.elyonut.wow

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

class PermissionsAdapter(var context: Context) : IPermissions, PermissionsListener {
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    private var hasPermissions = false

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            getLocationPermissions()
        } else {
            //log
        }
    }

    override fun getLocationPermissions(): Boolean {
        if ((PermissionsManager.areLocationPermissionsGranted(context))
            && (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            hasPermissions = true
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(context as Activity?)
        }

        return hasPermissions
    }
}