package ru.uporov.d.android.dakker.service

import android.app.Service
import android.content.Intent
import ru.uporov.d.android.common.Destroyable
import ru.uporov.d.android.common.OnDestroyObserver

class CustomService : Service(), Destroyable {

    override fun onBind(intent: Intent?) = null

    private val observers = mutableSetOf<OnDestroyObserver>()

    override fun addObserver(onDestroyObserver: OnDestroyObserver) {
        observers.add(onDestroyObserver)
    }

    override fun removeObserver(onDestroyObserver: OnDestroyObserver) {
        observers.remove(onDestroyObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        observers.forEach(OnDestroyObserver::onDestroy)
    }
}