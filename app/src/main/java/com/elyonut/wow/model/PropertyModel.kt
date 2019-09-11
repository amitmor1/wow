package com.elyonut.wow.model

import kotlin.reflect.KClass

data class PropertyModel(val name:String, val type: KClass<*>)
