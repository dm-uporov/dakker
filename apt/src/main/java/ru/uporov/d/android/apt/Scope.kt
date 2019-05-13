package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName

data class Scope(
    val core: ClassName,
    val providedDependencies: Set<Dependency>
)