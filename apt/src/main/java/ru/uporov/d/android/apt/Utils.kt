package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.ClassName
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol

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


fun List<Attribute.Class>.toKClassList(): List<ClassName> {
    return map { it.classType.toString() }
        .map {
            val name = it.split('.').last()
            ClassName(it.substringBefore(".$name"), name)

        }
}

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
