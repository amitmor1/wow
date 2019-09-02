package com.elyonut.wow

enum class NumericFilterTypes(val hebrewName: String) {
    RANGE("טווח"),
    GREATER("ערך מינימלי"),
    LOWER("ערך מקסימלי"),
    SPECIFIC("בחר ערך מסוים")
}