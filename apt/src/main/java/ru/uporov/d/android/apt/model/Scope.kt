package ru.uporov.d.android.apt.model

import com.squareup.kotlinpoet.ClassName

data class Scope(
    val core: ClassName,
    val providedDependencies: Set<Dependency>
)