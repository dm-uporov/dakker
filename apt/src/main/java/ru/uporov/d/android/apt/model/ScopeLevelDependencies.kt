package ru.uporov.d.android.apt.model

import com.squareup.kotlinpoet.ClassName

data class ScopeLevelDependencies(
    val withProviders: Map<ClassName, Set<Dependency>>,
    val withoutProviders: Map<ClassName, Set<Dependency>>
)