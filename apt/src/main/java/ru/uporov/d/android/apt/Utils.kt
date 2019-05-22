package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.util.Name
import ru.uporov.d.android.apt.model.Dependency
import ru.uporov.d.android.common.exception.GenericInDependencyException
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

fun Symbol.asDependency(parentClassName: Name): Dependency {
    if (qualifiedName.toString() == "error.NonExistentClass") {
        throw RuntimeException("Wrong $parentClassName class definition")
    }
    return Dependency(type.asTypeName().javaToKotlinType())
}

fun Symbol.ClassSymbol.asDependency(isSinglePerScope: Boolean) = asDependency(null, isSinglePerScope)

fun Symbol.MethodSymbol.asDependency(isSinglePerScope: Boolean) =
    asDependency(paramsAsDependencies(), isSinglePerScope)

private fun Symbol.asDependency(params: List<Dependency>?, isSinglePerScope: Boolean) =
    Dependency(
        enclClass().asType().asTypeName().javaToKotlinType(),
        isSinglePerScope,
        params
    )

fun Symbol.MethodSymbol.paramsAsDependencies(): List<Dependency> {
    val params = params()
    if (params.isNullOrEmpty()) return emptyList()

    return params.map { it.asDependency(qualifiedName) }
}


fun Type.ClassType.toClassName(): ClassName {
    val type = toString()
    return ClassName(type.substringBeforeLast("."), type.substringAfterLast("."))
}

private const val MODULE_FIELD_NAME_FORMAT = "%sModule"

fun ClassName.moduleName() = MODULE_FIELD_NAME_FORMAT.format(simpleName)

fun ClassName.moduleClassName() = ClassName(packageName, moduleName())

fun TypeName.flatGenerics(): String = toString().flatGenerics()

fun TypeName.javaToKotlinType(): TypeName =
    if (this is ParameterizedTypeName) {
        (rawType.javaToKotlinType() as ClassName).parameterizedBy(
            *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
        )
    } else {
        val className = JavaToKotlinClassMap.INSTANCE
            .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
        if (className == null) this
        else ClassName.bestGuess(className)
    }

private fun String.flatGenerics(): String {
    val qualifiedName = substringBefore("<")
    if (!qualifiedName.contains(".")) throw GenericInDependencyException(this)

    val name = qualifiedName.substringAfterLast(".")
    val intoGeneric = substringAfter("<", "").substringBeforeLast(">", "")
    return if (intoGeneric.isBlank()) {
        name
    } else {
        try {
            "$name${intoGeneric.split(",").joinToString(separator = "") { it.flatGenerics() }}"
        } catch (e: GenericInDependencyException) {
            throw GenericInDependencyException(this)
        }
    }
}