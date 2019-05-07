package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionRoot
import ru.uporov.d.android.common.PerApplication
import ru.uporov.d.android.common.exception.DependenciesConflictException
import ru.uporov.d.android.common.exception.IllegalAnnotationUsageException
import ru.uporov.d.android.common.exception.MoreThanOneInjectionRootException
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(DakkerProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class DakkerProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(
            InjectionRoot::class.java.name,
            PerApplication::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }


    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        val scopeDependencies = mutableSetOf<Dependency>()
        val scopeDependenciesWithoutProviders = mutableSetOf<Dependency>()

        roundEnvironment.getElementsAnnotatedWith(PerApplication::class.java)?.forEach { element ->
            when (element) {
                is Symbol.MethodSymbol ->
                    element
                        .asDependency()
                        .run(scopeDependencies::add)
                        .let { wasProviderAddedToCollection(it, element.enclClass()) }
                is Symbol.ClassSymbol ->
                    element
                        .members_field
                        .elements
                        .asSequence()
                        .filter { it is Symbol.MethodSymbol }
                        .map { it as Symbol.MethodSymbol }
                        .filter { it.name.toString() == "<init>" }
                        .also { constructors ->
                            val count = constructors.count()
                            if (count > 1) {
                                element
                                    .asDependency()
                                    .run(scopeDependenciesWithoutProviders::add)
                                    .let { wasProviderAddedToCollection(it, element) }
                            } else if (count == 1) {
                                constructors
                                    .first()
                                    .asDependency()
                                    .run(scopeDependencies::add)
                                    .let { wasProviderAddedToCollection(it, element) }
                            }
                        }
            }
        }

        val injectionRoots = roundEnvironment.getElementsAnnotatedWith(InjectionRoot::class.java)
        if (injectionRoots?.count() ?: 0 > 1) {
            throw MoreThanOneInjectionRootException()
        }
        injectionRoots?.forEach { element ->
            if (element !is Symbol.ClassSymbol) throw IllegalAnnotationUsageException(
                InjectionRoot::class
            )
            BeanBuilder(
                pack = processingEnv.elementUtils.getPackageOf(element).toString(),
                rootName = element.simpleName.toString(),
                scopeDependencies = scopeDependencies,
                scopeDependenciesWithoutProviders = scopeDependenciesWithoutProviders,
                requestedDependencies = element.getRequestedDependencies()
            )
                .build()
                .write()

        }
        return true
    }

    private fun wasProviderAddedToCollection(wasAdded: Boolean, element: Symbol.ClassSymbol) {
        if (!wasAdded) throw DependenciesConflictException(element.qualifiedName.toString())
    }

    private fun Symbol.ClassSymbol.getRequestedDependencies(): List<Dependency> {
        return enclosedElements
            .filter { it.getAnnotation(Inject::class.java) != null }
            .map { it.type.returnType.asTypeName() }
            .map(TypeName::toString)
            .map {
                Dependency(
                    it.substringBeforeLast("."),
                    it.substringAfterLast(".")
                )
            }
    }

    private fun FileSpec.write() = writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))

    private fun Symbol.ClassSymbol.asDependency() = asDependency(null)

    private fun Symbol.MethodSymbol.asDependency() = asDependency(paramsAsDependencies())

    private fun Symbol.asDependency(params: List<Dependency>?) =
        Dependency(
            processingEnv.elementUtils.getPackageOf(this).toString(),
            enclClass().simpleName.toString(),
            params
        )


    override fun hashCode(): Int {
        return 1
    }
}
