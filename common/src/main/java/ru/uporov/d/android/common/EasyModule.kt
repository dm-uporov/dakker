package ru.uporov.d.android.common

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class EasyModule(
    val providersFor: Array<KClass<*>>
)