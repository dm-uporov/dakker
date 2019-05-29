package ru.uporov.d.android.apt

import androidx.lifecycle.LifecycleOwner
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import com.sun.tools.javac.util.Name
import ru.uporov.d.android.apt.model.Dependency
import ru.uporov.d.android.common.Destroyable
import ru.uporov.d.android.common.exception.GenericInDependencyException
import ru.uporov.d.android.common.exception.IncorrectCoreOfScopeException
import javax.lang.model.element.Element
import kotlin.reflect.KClass
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
            *typeArguments.map(TypeName::javaToKotlinType).toTypedArray()
        )
    } else {
        JavaToKotlinClassMap.INSTANCE
            .mapJavaToKotlin(FqName(toString()))
            ?.asSingleFqName()
            ?.asString()
            ?.let(ClassName.Companion::bestGuess)
            ?: this
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

fun Set<Pair<Int, Dependency>>.toGroupedMap() = asSequence()
    .groupBy(Pair<Int, Dependency>::first) { it.second }
    .mapValues { it.value.toSet() }

fun Element.toClassSymbol(): Symbol.ClassSymbol? {
    return if (this is Symbol.ClassSymbol) this else null
}

fun Element.toClassName() = ClassName.bestGuess(asType().asTypeName().toString())

fun Symbol.ClassSymbol.checkOnDestroyable() {
    if (!isImplementedOneOfInterfaces(LifecycleOwner::class, Destroyable::class)) {
        throw IncorrectCoreOfScopeException(className())
    }
}

private fun Symbol.ClassSymbol.isImplementedOneOfInterfaces(vararg interfacesToImpl: KClass<*>) : Boolean {
    if (interfaces.nonEmpty()) {
        interfaces.find { it.isKindOfOneOf(*interfacesToImpl) }?.run { return true }
    }

    if (superclass == Type.noType) return false

    return (superclass.tsym as? Symbol.ClassSymbol)?.isImplementedOneOfInterfaces(*interfacesToImpl) ?: false
}

private fun Type.isKindOfOneOf(vararg cls: KClass<*>): Boolean {
    if (this !is Type.ClassType) return false

    cls.forEach {
        if (toClassName() == it.asClassName()) return true
    }

    return interfaces_field?.find { isKindOfOneOf(*cls) } != null
}