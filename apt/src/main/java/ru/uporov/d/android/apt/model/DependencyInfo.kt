package ru.uporov.d.android.apt.model

data class DependencyInfo(
    val scopeId: Int,
    val isSinglePerScope: Boolean = true
)