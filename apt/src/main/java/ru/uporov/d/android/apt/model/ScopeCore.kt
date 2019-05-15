package ru.uporov.d.android.apt.model

import com.squareup.kotlinpoet.ClassName

data class ScopeCore(
    val coreClass: ClassName,
    val parentScopeCoreClass: ClassName?,
    val requestedDependencies: Set<Dependency>
)