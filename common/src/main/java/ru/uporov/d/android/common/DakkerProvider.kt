package ru.uporov.d.android.common

import kotlin.reflect.KClass

abstract class DakkerProvider<T: Any> {

    abstract val beanClass: KClass<T>
    abstract val branches: Set<DakkerProvider<*>>
    abstract val providers: ProvidersMap<T, *>

    fun findProviderFor(dependency: KClass<*>): DakkerProvider<*>? {
        return if (containsProviderFor(dependency)) {
            this
        } else {
            branches.firstOrNull { it.containsProviderFor(dependency) }
        }
    }

    private fun containsProviderFor(dependency: KClass<*>): Boolean {
        return providers.containsKey(dependency)
    }
}