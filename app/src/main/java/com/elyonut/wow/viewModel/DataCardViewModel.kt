package com.elyonut.wow.viewModel

import android.app.Application
import android.content.res.Resources
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class DataCardViewModel(application: Application) : AndroidViewModel(application) {

    var isReadMoreButtonClicked = MutableLiveData<Boolean>()
    var shouldCloseButton = MutableLiveData<Boolean>()

    fun readMoreButtonClicked(moreContentView: View) {
        isReadMoreButtonClicked.value = moreContentView.visibility == View.GONE
    }

    fun close() {
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