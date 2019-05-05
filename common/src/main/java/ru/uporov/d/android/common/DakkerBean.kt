package ru.uporov.d.android.common

import kotlin.reflect.KClass

abstract class DakkerBean<T: Any> {

    abstract val beanClass: KClass<T>
    abstract val providers: ProvidersMap<T, *>
}