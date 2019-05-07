package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName

data class Scope(
    val coreClassName: ClassName?,
    val scopeDependencies: Set<Dependency>,
    val scopeDependenciesWithoutProviders: Set<Dependency>
)