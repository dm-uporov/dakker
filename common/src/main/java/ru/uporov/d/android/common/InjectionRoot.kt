package ru.uporov.d.android.common

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class InjectionRoot(
    val branches: Array<KClass<out Bean>> = [],
    val dependencies: Array<KClass<*>> = []
)
