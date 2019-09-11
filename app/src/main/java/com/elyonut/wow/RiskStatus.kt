package com.elyonut.wow

import android.graphics.Color

enum class RiskStatus(val text: String, val color: Int) {
    NONE("אפור", Color.GRAY),
    LOW("צהוב", Color.parseColor("#ffdb4d")),
//    MEDIUM("כתום", Color.parseColor("#f5ad42")),
    MEDIUM("כתום", R.color.lowRisk),
    HIGH("אדום", Color.RED)
}