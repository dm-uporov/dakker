package ru.uporov.d.android.common

interface Destroyable {

    fun addObserver(onDestroyObserver: OnDestroyObserver)

    fun removeObserver(onDestroyObserver: OnDestroyObserver)
}

interface OnDestroyObserver {

    fun onDestroy()
}