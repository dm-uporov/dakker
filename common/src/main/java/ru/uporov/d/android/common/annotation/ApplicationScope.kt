package ru.uporov.d.android.common.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class ApplicationScope(
    val isSinglePerScope: Boolean = true
)