package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName

data class ScopeLevel(
    val nodes: Set<ClassName>,
    val providedDependencies: Set<Dependency>
)