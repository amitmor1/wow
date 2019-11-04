package com.elyonut.wow.model

class Greeting() {
    lateinit var id: Number
    lateinit var content: String

    constructor(id: Number, content: String) : this() {
        this.id = id
        this.content = content
    }
}