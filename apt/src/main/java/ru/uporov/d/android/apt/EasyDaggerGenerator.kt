package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol
import ru.uporov.d.android.common.EasyComponent
import ru.uporov.d.android.common.EasyDagger
import ru.uporov.d.android.common.EasyModule
import ru.uporov.d.android.common.exception.IllegalAnnotationUsageException
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(EasyDaggerGenerator.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class EasyDaggerGenerator : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(EasyDagger::class.java.name)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        roundEnvironment.getElementsAnnotatedWith(EasyDagger::class.java)?.forEach { element ->
            if (element !is Symbol.ClassSymbol) throw IllegalAnnotationUsageException(EasyDagger::class)

            val pack = processingEnv.elementUtils.getPackageOf(element).toString()
            val className = element.simpleName.toString()

            val scope = generateScope(pack, className)

            val modules = mutableSetOf<String>()
            for (enclosed in element.enclosedElements) {
                if (enclosed.getAnnotation(EasyComponent::class.java) != null) {
                    for (annotation in enclosed.annotationMirrors) {
                        for (pair in annotation.values) {
                            when (pair.fst.simpleName.toString()) {
                                // TODO generate subcomponent
                            }
                        }
                    }
                }

                if (enclosed.getAnnotation(EasyModule::class.java) != null) {
                    for (annotation in enclosed.annotationMirrors) {
                        for (pair in annotation.values) {
                            when (pair.fst.simpleName.toString()) {
                                "providersFor" -> generateModule(
                                    pack,
                                    enclosed.simpleName.toString(),
                                    pair.snd.value as List<Attribute.Class>,
                                    scope
                                ).let(modules::add)
                            }
                        }
                    }
                }
            }
            generateComponent(pack, className, modules, scope)

//                if (it is Symbol.MethodSymbol) {
//                    content += it.javaClass.canonicalName +" "
//                    content += it.code.toString()
//
////                    it.returnType - qualified name of return type class
//                }
//
//                it.annotationMirrors
//                    .forEach { mirror ->
//                        mirror.elementValues.forEach { key , value ->
//                            when(key.simpleName.toString()) {
//                                "provide" -> {
//                                    val typeMirror = value.value as TypeMirror
//                                    content = typeMirror.javaClass.toGenericString()
//                                } //as Class<FU>).typeParameters?.forEach { param -> content += param.name }
//                            }
//                        }
//
//                    }
//                val provided = it.getAnnotation(EasyDagger::class.java).provide
//                provided.typeParameters.forEach { param -> content += param.name }
//
//                generateClass(className, pack, content)
        }
        return true
    }

    private fun generateScope(pack: String, className: String): String {
        val fileName = scopeName(className)

        FileSpec.builder(pack, fileName)
            .addType(
                TypeSpec.annotationBuilder(fileName)
                    .addAnnotation(ClassName("javax.inject", "Scope"))
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("kotlin.annotation", "Retention"))
                            .addMember("kotlin.annotation.AnnotationRetention.RUNTIME")
                            .build()
                    )
                    .build()
            )
            .build()
            .write()

        return fileName
    }

    private fun generateComponent(
        pack: String,
        className: String,
        modules: Set<String>,
        scope: String
    ) {
        val fileName = componentName(className)

        FileSpec.builder(pack, fileName)
            .addImport(pack, *modules.toTypedArray())
            .addType(
                TypeSpec.interfaceBuilder(fileName)
                    .addAnnotation(ClassName(pack, scope))
                    .addAnnotation(
                        AnnotationSpec.builder(ClassName("dagger", "Component"))
                            .addMember("modules = [${modules.joinToString { "$it::class" }}]")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("inject")
                            .addModifiers(KModifier.ABSTRACT)
                            // todo remove
                            .addParameter("activity", ClassName("ru.uporov.d.android.easydagger", "MainActivity"))
                            .build()
                    )
                    .build()
            )
            .build()
            .write()
    }

    private fun generateModule(
        pack: String,
        functionName: String,
        dependencies: List<Attribute.Class>,
        scope: String?
    ): String {
        val fileName = moduleName(scope, functionName)


        FileSpec.builder(pack, fileName)
            .addType(
                TypeSpec.classBuilder(fileName)
                    .addAnnotation(ClassName("dagger", "Module"))
                    .apply {
                        dependencies
                            .map { it.classType.toString() }
                            .map {
                                val name = it.split('.').last()
                                ClassName(it.substringBefore(".$name"), name)
                            }
                            .forEach { className ->
                                addFunction(
                                    FunSpec.builder("provide${className.simpleName}")
                                        .addAnnotation(ClassName("dagger", "Provides"))
                                        .let {
                                            scope ?: return@let it
                                            it.addAnnotation(ClassName(pack, scope))
                                        }
                                        .returns(className)
                                        .addStatement("return ru.uporov.d.android.common.EasyDaggerContext.instance.component.provide()")
                                        .build()
                                )
                            }
                    }
                    .build()
            )
            .build()
            .write()

        return fileName
    }

    private fun scopeName(className: String) = "${className}Scope"

    private fun componentName(className: String) = "${className}Component"

    private fun moduleName(scope: String?, functionName: String) = "${scope ?: "NoScope"}_${functionName}Module"

    private fun FileSpec.write() = writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))

    override fun hashCode(): Int {
        return 1
    }
}