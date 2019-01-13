package ru.uporov.d.android.common

import kotlin.reflect.KClass

interface DependencyLocator {

    fun <T: Any> provide(type: KClass<T>, vararg args: Any): T
}