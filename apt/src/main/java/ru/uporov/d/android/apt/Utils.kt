package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type

fun Symbol.MethodSymbol.paramsAsDependencies(): List<Dependency> {
    val params = params()
    if (params.isNullOrEmpty()) return emptyList()

    return params.map {
        val type = it.type.toString()
        Dependency(
            type.substringBeforeLast("."),
            type.substringAfterLast(".")
        )
    }
}

fun Type.ClassType.toKClassList(): ClassName {
    val type = toString()
    return ClassName(type.substringBeforeLast("."), type.substringAfterLast("."))
}

private const val NODE_FIELD_NAME_FORMAT = "%sNode"

fun ClassName.nodeName() = NODE_FIELD_NAME_FORMAT.format(simpleName)

fun ClassName.nodeClassName() = ClassName(packageName, nodeName())


//    fun Symbol.ClassSymbol.getClassesFromAnnotationParams() {
//        val branches = mutableSetOf<ClassName>()
//        val dependencies = mutableSetOf<ClassName>()
//        for (annotation in annotationMirrors) {
//            for (pair in annotation.values) {
//                when (pair.fst.simpleName.toString()) {
//                    "branches" -> (pair.snd.value as? List<Attribute.Class>)
//                        ?.toKClassList()
//                        ?.let {
//                            branches.addAll(it)
//                        }
//                    "dependencies" -> (pair.snd.value as? List<Attribute.Class>)
//                        ?.toKClassList()
//                        ?.let {
//                            dependencies.addAll(it)
//                        }
//                }
//            }
//        }
//    }
