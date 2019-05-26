package ru.uporov.d.android.common.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DakkerScope(
    val scopeId: Int,
    val isSinglePerScope: Boolean = true
)