package ru.uporov.d.android.apt.model

import com.squareup.kotlinpoet.ClassName

data class DependencyInfo(
    val scopeCoreClass: ClassName?,
    val isSinglePerScope: Boolean = true
)