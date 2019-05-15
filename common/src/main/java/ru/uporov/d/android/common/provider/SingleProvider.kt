package ru.uporov.d.android.common.provider

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent

// Provider for scope single dependency
fun <O, T> single(provide: (O) -> T) = SingleProvider(provide)

class SingleProvider<O, T> internal constructor(
    private val provider: (O) -> T
) : Provider<O, T> {

    private val ownersHashesToValuesMap = hashMapOf<Int, T>()

    override fun invoke(scopeOwner: O): T {
        synchronized(this) {
            val ownerHash = scopeOwner.hashCode()
            with(ownersHashesToValuesMap[ownerHash]) {
                if (this == null) {
                    subscribeOnLifecycle(scopeOwner)
                    val newValue = provider(scopeOwner)
                    ownersHashesToValuesMap[ownerHash] = newValue
                    return newValue
                }
                return this
            }
        }
    }

    private fun O.trashValue() {
        synchronized(this@SingleProvider) {
            ownersHashesToValuesMap.remove(hashCode())
        }
    }

    private fun subscribeOnLifecycle(scopeOwner: O) {
        if (scopeOwner is LifecycleOwner) {
            val lifecycle = scopeOwner.lifecycle
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    lifecycle.removeObserver(this)
                    scopeOwner.trashValue()
                }
            })
        }
    }
}