package ru.uporov.d.android.common

import ru.uporov.d.android.common.exception.CannotFindDependencyException
import kotlin.reflect.KClass

class EasyDaggerModule private constructor() {

    companion object {
        fun module(init: EasyDaggerModule.() -> Unit): EasyDaggerModule {
            return EasyDaggerModule().apply(init)
        }
    }

    val providers = mutableMapOf<Any, () -> Any>()

    inline fun <reified T : Any> provider(noinline provide: () -> T) {
        providers[T::class] = provide
    }

    fun <T: Any> get(type: KClass<T>): T =
        providers[type]?.invoke() as? T ?: throw CannotFindDependencyException(type)

    inline fun <reified T> get(): T =
        providers[T::class]?.invoke() as? T ?: throw CannotFindDependencyException(T::class)
}