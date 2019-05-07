package ru.uporov.d.android.common

import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class NodeScope(
    val coreClass: KClass<out LifecycleOwner>
)