package com.elyonut.wow.model

data class AlertModel(val id: String, val message: String, val image: Int, val time: String ,var isRead: Boolean = false)