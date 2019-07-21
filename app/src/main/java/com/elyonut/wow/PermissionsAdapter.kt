package com.elyonut.wow

import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

class PermissionsAdapter: IPermissions, PermissionsListener {
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPermissionResult(granted: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var permissionsManager: PermissionsManager = PermissionsManager(this)

    override fun getLocationPermissions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}