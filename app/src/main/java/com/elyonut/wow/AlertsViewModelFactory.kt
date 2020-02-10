package com.elyonut.wow

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.elyonut.wow.view.AlertsFragment
import com.elyonut.wow.viewModel.AlertsViewModel

class AlertsViewModelFactory(val application: Application, var alertsManager: AlertsManager, var onClickHandler: AlertsFragment.OnClickInterface): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return  AlertsViewModel(application, alertsManager, onClickHandler) as T
    }
}