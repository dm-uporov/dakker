package ru.uporov.d.android.apt.builder

import androidx.lifecycle.LifecycleOwner
import com.squareup.kotlinpoet.*
import ru.uporov.d.android.apt.nodeClassName
import ru.uporov.d.android.apt.nodeName
import ru.uporov.d.android.common.Node
import ru.uporov.d.android.common.exception.DakkerIsNotInitializedException

internal const val DAKKER_FILE_NAME = "Dakker"

class DakkerBuilder(
    private val root: ClassName,
    private val nodesCores: Set<ClassName>
) {

    private val nodes: Set<ClassName> = nodesCores.map(ClassName::nodeClassName).toSet()
    private val nodesFields = setOf(root.nodeClassName()).union(nodes)
        .map { it.simpleName.decapitalize() to it }
        .toMap()

    fun build(): FileSpec {
        return FileSpec.builder(root.packageName, DAKKER_FILE_NAME)
            .addImport(
                "androidx.lifecycle",
                "LifecycleObserver", "LifecycleOwner", "OnLifecycleEvent", "Lifecycle"
            )
            .apply { nodesCores.forEach { addImport(it.packageName, it.simpleName) } }
            .addType(
                TypeSpec.objectBuilder(DAKKER_FILE_NAME)
                    .nodesLateinitProperties()
                    .startDakkerFunction()
                    .nodesGetters()
                    .build()
            )
            .bindScopeToLifecycleFunction()
            .bindScopeFunction()
            .build()
    }

    private fun TypeSpec.Builder.nodesLateinitProperties() = apply {
        nodesFields.forEach {
            addProperty(
                PropertySpec.builder(
                    it.key,
                    it.value,
                    KModifier.PRIVATE,
                    KModifier.LATEINIT
                )
                    .mutable(true)
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.startDakkerFunction() = apply {
        addFunction(
            FunSpec.builder("startDakker")
                .receiver(root)
                .apply {
                    val codeBuilder = CodeBlock.builder()
                    nodesFields.forEach {
                        addParameter(ParameterSpec.builder(it.key, it.value).build())
                        codeBuilder.addStatement("$DAKKER_FILE_NAME.${it.key} = ${it.key}")
                    }
                    addCode(codeBuilder.build())
                }
                .build()
        )
    }

    private fun FileSpec.Builder.bindScopeToLifecycleFunction() = apply {
        addFunction(
            FunSpec.builder("bindScopeToLifecycle")
                .receiver(LifecycleOwner::class)
                .addStatement("when (this) {")
                .apply {
                    nodesCores.forEach {
                        addStatement("is ${it.simpleName} -> bindScope(Dakker::get${it.nodeName()})")
                    }
                }
                .addStatement("}")
                .build()
        )
    }

    private fun FileSpec.Builder.bindScopeFunction() = apply {
        addFunction(
            FunSpec.builder("bindScope")
                .addModifiers(KModifier.PRIVATE)
                .receiver(LifecycleOwner::class)
                .addParameter("node", LambdaTypeName.get(returnType = Node::class.asTypeName()))
                .addCode(
                    """
                    val lifecycle = getLifecycle()
                    lifecycle.addObserver(object : LifecycleObserver {
                        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                        fun onDestroy() {
                            lifecycle.removeObserver(this)
                            node().trash()
                        }
                    })
                    """.trimIndent()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.nodesGetters() = apply {
        nodesFields.forEach {
            addFunction(
                FunSpec.builder("get${it.key.capitalize()}")
                    .returns(it.value)
                    .addCode(
                        """
                        if (!::${it.key}.isInitialized) throw ${DakkerIsNotInitializedException::class.qualifiedName}()

                        return ${it.key}
                    """.trimIndent()
                    )
                    .build()
            )
        }
    }
}