package ru.uporov.d.android.common

object Stub {

    fun <T> injectionStub(): Lazy<T> {
        throw RuntimeException()
    }
}