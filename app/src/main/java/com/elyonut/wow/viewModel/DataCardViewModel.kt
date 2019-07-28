package com.elyonut.wow.viewModel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout

class DataCardViewModel(application: Application) : AndroidViewModel(application) {

    var isReadMoreButtonClicked = MutableLiveData<Boolean>()
    var shouldCloseButton = MutableLiveData<Boolean>()

    fun readMoreButtonClicked(moreContentView: View) {
        isReadMoreButtonClicked.value = moreContentView.visibility == View.GONE
    }

    fun closeButtonClicked() {
        shouldCloseButton.value = true
    }

    fun getRelativeLayoutParams(sizeRelativelyToScreen: Double): FrameLayout.LayoutParams {
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            (getDeviceHeight() * sizeRelativelyToScreen).toInt()
        )
    }

    private fun getDeviceHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }
}