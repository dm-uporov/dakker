package ru.uporov.d.android.easydagger

import android.content.Context

class SomeInteractor(private val context: Context) {

    val some: Any? = null
    val activity: MainActivity? = null

    fun someInteresting(hello: String): MainActivity? {
        println("Hello world!")
        return null
    }
}