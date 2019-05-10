package ru.uporov.d.android.common.provider

interface Provider<O, T> : (O) -> T {

    fun trashValue() {}
}