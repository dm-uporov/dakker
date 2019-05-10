package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName

data class Dependency(
    val pack: String,
    val name: String,
    val isSinglePerScope: Boolean = false,
    val params: List<Dependency>? = null
) {
    val qualifiedName = "$pack.$name"
    val className by lazy { ClassName.bestGuess(qualifiedName) }

    override fun equals(other: Any?): Boolean {
        if (other !is Dependency) return false
        return qualifiedName == other.qualifiedName
    }

    override fun hashCode(): Int {
        return qualifiedName.hashCode()
    }
}