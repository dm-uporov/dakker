package ru.uporov.d.android.common.provider

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import ru.uporov.d.android.common.Destroyable
import ru.uporov.d.android.common.OnDestroyObserver

// Provider for scope single dependency
fun <O : Any, T> single(provide: (O) -> T) = SingleProvider(provide)

class SingleProvider<O, T> internal constructor(
    private val provider: (O) -> T
) : Provider<O, T> {

    private val ownersHashesToValuesMap = hashMapOf<Int, T>()

    override fun invoke(scopeOwner: O): T {
        synchronized(this) {
            val ownerHash = scopeOwner.hashCode()
            with(ownersHashesToValuesMap[ownerHash]) {
                if (this == null) {
                    subscribeOnDestroyCallback(scopeOwner)
                    val newValue = provider(scopeOwner)
                    ownersHashesToValuesMap[ownerHash] = newValue
                    return newValue
                }
                return this
            }
        }
    }

    private fun subscribeOnDestroyCallback(scopeOwner: O) {
        when (scopeOwner) {
            is LifecycleOwner -> subscribeOnLifecycle(scopeOwner)
            is Destroyable -> subscribeOnDestroyable(scopeOwner)
        }
    }

    private fun subscribeOnLifecycle(scopeOwner: LifecycleOwner) {
        val lifecycle = scopeOwner.lifecycle
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                lifecycle.removeObserver(this)
                scopeOwner.trashValue()
            }
        })
    }

    private fun subscribeOnDestroyable(scopeOwner: Destroyable) {
        object : OnDestroyObserver {
            override fun onDestroy() {
                scopeOwner.removeObserver(this)
                scopeOwner.trashValue()
            }
        }.run {
            scopeOwner.addObserver(this)
        }
    }

    private fun Any.trashValue() {
        synchronized(this@SingleProvider) {
            ownersHashesToValuesMap.remove(hashCode())
        }
    }
}