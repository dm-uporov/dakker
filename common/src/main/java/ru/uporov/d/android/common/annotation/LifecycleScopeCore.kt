package ru.uporov.d.android.common.annotation

import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class LifecycleScopeCore(
    // if it is not defined, parent scope will be ApplicationScope
    val parentScopeCoreClass: KClass<out LifecycleOwner> = Nothing::class
)
