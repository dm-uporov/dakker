package ru.uporov.d.android.common

import ru.uporov.d.android.common.exception.CannotFindDependencyException
import kotlin.reflect.KClass

class EasyDaggerComponent private constructor(): DependencyLocator {

    companion object {
        fun component(init: EasyDaggerComponent.() -> Unit): EasyDaggerComponent {
            return EasyDaggerComponent().apply(init)
        }
    }

    private val modules = mutableSetOf<EasyDaggerModule>()
    private val components = mutableSetOf<EasyDaggerComponent>()

    fun module(init: EasyDaggerModule.() -> Unit) {
        modules.add(EasyDaggerModule.module(init))
    }

    // Subcomponents
    fun component(init: EasyDaggerComponent.() -> Unit) {
        components.add(Companion.component(init))
    }

    inline fun <reified T: Any> provide(vararg args: Any): T {
        return provide(T::class, *args)
    }

    override fun <T: Any> provide(type: KClass<T>, vararg args: Any): T {
        return modules.firstOrNull {
            it.providers[type] != null
        }?.get(type) ?: throw CannotFindDependencyException(type)
    }
}
