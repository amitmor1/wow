package com.elyonut.wow

enum class NumericFilterTypes(val hName: String) {
    RANGE("טווח"),
    GREATER("גדול מ"),
    LOWER("קטן מ"),
    SPECIFIC("בחר ערך מסוים")
}