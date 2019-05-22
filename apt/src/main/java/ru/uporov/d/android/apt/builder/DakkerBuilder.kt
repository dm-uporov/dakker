package ru.uporov.d.android.apt.builder

import com.squareup.kotlinpoet.*
import ru.uporov.d.android.apt.moduleClassName
import ru.uporov.d.android.common.exception.DakkerIsNotInitializedException

internal const val DAKKER_FILE_NAME = "Dakker"

class DakkerBuilder(
    private val root: ClassName,
    private val modulesCores: Set<ClassName>
) {

    private val modules: Set<ClassName> = modulesCores.map(ClassName::moduleClassName).toSet()
    private val modulesFields = setOf(root.moduleClassName()).union(modules)
        .map { it.simpleName.decapitalize() to it }
        .toMap()

    fun build(): FileSpec {
        return FileSpec.builder(root.packageName, DAKKER_FILE_NAME)
            .addImport(
                "androidx.lifecycle",
                "LifecycleObserver", "LifecycleOwner", "OnLifecycleEvent", "Lifecycle"
            )
            .apply { modulesCores.forEach { addImport(it.packageName, it.simpleName) } }
            .addType(
                TypeSpec.objectBuilder(DAKKER_FILE_NAME)
                    .modulesLateinitProperties()
                    .startDakkerFunction()
                    .nodesGetters()
                    .build()
            )
            .build()
    }

    private fun TypeSpec.Builder.modulesLateinitProperties() = apply {
        modulesFields.forEach {
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
                    modulesFields.forEach {
                        addParameter(ParameterSpec.builder(it.key, it.value).build())
                        codeBuilder.addStatement("$DAKKER_FILE_NAME.${it.key} = ${it.key}")
                    }
                    addCode(codeBuilder.build())
                }
                .build()
        )
    }

    private fun TypeSpec.Builder.nodesGetters() = apply {
        modulesFields.forEach {
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