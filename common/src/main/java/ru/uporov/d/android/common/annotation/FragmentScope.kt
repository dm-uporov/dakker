package ru.uporov.d.android.common.annotation

import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class FragmentScope(
    val coreClass: KClass<out LifecycleOwner>,
    val isSinglePerScope: Boolean = true
)