package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import ru.uporov.d.android.common.*
import ru.uporov.d.android.common.exception.DependenciesConflictException
import ru.uporov.d.android.common.exception.IllegalAnnotationUsageException
import ru.uporov.d.android.common.exception.MoreThanOneInjectionRootException
import ru.uporov.d.android.common.exception.NoInjectionRootException
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

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
            ApplicationScope::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }


    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        roundEnvironment.generateScopesBy(InjectionNode::class, NodeScope::class)
        roundEnvironment.generateScopesBy(InjectionRoot::class, ApplicationScope::class)


//        val scopeDependencies = mutableSetOf<Dependency>()
//        val scopeDependenciesWithoutProviders = mutableSetOf<Dependency>()
//
//        roundEnvironment.getElementsAnnotatedWith(ApplicationScope::class.java)?.forEach { element ->
//            when (element) {
//                is Symbol.MethodSymbol ->
//                    element
//                        .asDependency()
//                        .run(scopeDependencies::add)
//                        .let { wasProviderAddedToCollection(it, element.enclClass()) }
//                is Symbol.ClassSymbol ->
//                    element
//                        .members_field
//                        .elements
//                        .asSequence()
//                        .filter { it is Symbol.MethodSymbol }
//                        .map { it as Symbol.MethodSymbol }
//                        .filter { it.name.toString() == "<init>" }
//                        .also { constructors ->
//                            val count = constructors.count()
//                            if (count > 1) {
//                                element
//                                    .asDependency()
//                                    .run(scopeDependenciesWithoutProviders::add)
//                                    .let { wasProviderAddedToCollection(it, element) }
//                            } else if (count == 1) {
//                                constructors
//                                    .first()
//                                    .asDependency()
//                                    .run(scopeDependencies::add)
//                                    .let { wasProviderAddedToCollection(it, element) }
//                            }
//                        }
//            }
//        }
//
//        val injectionRoots = roundEnvironment.getElementsAnnotatedWith(InjectionRoot::class.java)
//
//        if (injectionRoots?.count() ?: 0 > 1) throw MoreThanOneInjectionRootException()
//
//        injectionRoots?.firstOrNull()?.let { element ->
//
//            if (element !is Symbol.ClassSymbol) throw IllegalAnnotationUsageException(InjectionRoot::class)
//
//            NodeBuilder(
//                pack = processingEnv.elementUtils.getPackageOf(element).toString(),
//                rootName = element.simpleName.toString(),
//                scopeDependencies = scopeDependencies,
//                scopeDependenciesWithoutProviders = scopeDependenciesWithoutProviders,
//                requestedDependencies = element.getRequestedDependencies()
//            )
//                .build()
//                .write()
//
//        }
        return true
    }

    private fun RoundEnvironment.generateScopesBy(
        coreMarker: KClass<out Annotation>,
        scopeLevel: KClass<out Annotation>
    ) {
        val scopeDependencies = mutableSetOf<Pair<ClassName?, Dependency>>()
        val scopeDependenciesWithoutProviders = mutableSetOf<Pair<ClassName?, Dependency>>()

        getElementsAnnotatedWith(scopeLevel.java)?.forEach { element ->
            if (element !is Symbol) return@forEach

            val className = element.getCoreClassNameOrNull()
            when (element) {
                is Symbol.MethodSymbol ->
                    element
                        .asDependency()
                        .let { scopeDependencies.add(className to it) }
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
                                    .let { scopeDependenciesWithoutProviders.add(className to it) }
                                    .let { wasProviderAddedToCollection(it, element) }
                            } else if (count == 1) {
                                constructors
                                    .first()
                                    .asDependency()
                                    .let { scopeDependencies.add(className to it) }
                                    .let { wasProviderAddedToCollection(it, element) }
                            }
                        }
            }
        }

        val scopeDependenciesMap = scopeDependencies
            .groupBy { it.first }
            .mapValues { it.value.map { pair -> pair.second }.toSet() }

        val scopeDependenciesWithoutProvidersMap = scopeDependenciesWithoutProviders
            .groupBy { it.first }
            .mapValues { it.value.map { pair -> pair.second }.toSet() }

        val injectionCores = getElementsAnnotatedWith(coreMarker.java) ?: emptySet()

        val rootClassName: ClassName? =
            if (coreMarker == InjectionRoot::class) {
                if (injectionCores.count() > 1) {
                    throw MoreThanOneInjectionRootException()
                } else {
                    injectionCores.firstOrNull()?.toClassName()
                }
            } else {
                null
            }

        val requestedDependenciesMap = mutableMapOf<ClassName, Set<Dependency>>()

        injectionCores
            .asSequence()
            .map {
                if (it is Symbol.ClassSymbol) {
                    return@map it as Symbol.ClassSymbol
                }
                throw IllegalAnnotationUsageException(coreMarker)
            }
            .map {
                return@map it.toClassName().also { name ->
                    requestedDependenciesMap[name] = it.getRequestedDependencies()
                }
            }
            .map { if (it == rootClassName) null else it }
            .toSet()
            .union(scopeDependenciesMap.keys)
            .union(scopeDependenciesWithoutProvidersMap.keys)
            .map {
                if (it == null) {
                    if (rootClassName == null) throw NoInjectionRootException()

                    NodeBuilder(
                        pack = rootClassName.packageName,
                        rootName = rootClassName.simpleName,
                        scopeDependencies = scopeDependenciesMap[null] ?: emptySet(),
                        scopeDependenciesWithoutProviders = scopeDependenciesWithoutProvidersMap[null]
                            ?: emptySet(),
                        requestedDependencies = requestedDependenciesMap[rootClassName] ?: emptySet()
                    )
                } else {
                    NodeBuilder(
                        pack = it.packageName,
                        rootName = it.simpleName,
                        scopeDependencies = scopeDependenciesMap[it] ?: emptySet(),
                        scopeDependenciesWithoutProviders = scopeDependenciesWithoutProvidersMap[it] ?: emptySet(),
                        requestedDependencies = requestedDependenciesMap[it] ?: emptySet()
                    )
                }
            }.forEach { it.build().write() }
    }

    private fun wasProviderAddedToCollection(wasAdded: Boolean, element: Symbol.ClassSymbol) {
        if (!wasAdded) throw DependenciesConflictException(element.qualifiedName.toString())
    }

    private fun Symbol.ClassSymbol.getRequestedDependencies(): Set<Dependency> {
        return enclosedElements
            .asSequence()
            .filter { it.getAnnotation(Inject::class.java) != null }
            .map { it.type.returnType.asTypeName() }
            .map(TypeName::toString)
            .map {
                Dependency(
                    it.substringBeforeLast("."),
                    it.substringAfterLast(".")
                )
            }
            .toSet()
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


    private fun Symbol.getCoreClassNameOrNull(): ClassName? {
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "coreClass" -> return (pair.snd.value as? Type.ClassType)?.toKClassList()
                }
            }
        }
        return null
    }

    private fun Element.toClassName() = ClassName(
        processingEnv.elementUtils.getPackageOf(this).toString(),
        simpleName.toString()
    )

    override fun hashCode(): Int {
        return 1
    }
}
