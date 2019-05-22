package ru.uporov.d.android.apt.model

import com.squareup.kotlinpoet.TypeName
import ru.uporov.d.android.apt.flatGenerics

data class Dependency constructor(
    val typeName: TypeName,
    val isSinglePerScope: Boolean = true,
    val params: List<Dependency>? = null
) {

    val uniqueName: String = typeName.flatGenerics()

    override fun equals(other: Any?): Boolean {
        if (other !is Dependency) return false
        return typeName == other.typeName
    }

    override fun hashCode(): Int {
        return typeName.hashCode()
    }
}