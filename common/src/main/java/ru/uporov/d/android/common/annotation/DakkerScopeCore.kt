package ru.uporov.d.android.common.annotation

import ru.uporov.d.android.common.APPLICATION_SCOPE_ID

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class DakkerScopeCore(
    val scopeId: Int,
    val parentScopeId: Int = APPLICATION_SCOPE_ID
)
