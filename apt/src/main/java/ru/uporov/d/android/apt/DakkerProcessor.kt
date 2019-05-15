package ru.uporov.d.android.apt

import androidx.lifecycle.LifecycleOwner
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import ru.uporov.d.android.apt.builder.DakkerBuilder
import ru.uporov.d.android.apt.builder.NodeBuilder
import ru.uporov.d.android.apt.model.*
import ru.uporov.d.android.common.annotation.*
import ru.uporov.d.android.common.exception.DependenciesConflictException
import ru.uporov.d.android.common.exception.IllegalAnnotationUsageException
import ru.uporov.d.android.common.exception.IncorrectCoreOfScopeException
import ru.uporov.d.android.common.exception.MoreThanOneInjectionRootException
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
            DakkerApplication::class.java.name,
            ApplicationScope::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        val root = roundEnvironment.getRoot() ?: return true
        val rootClassName = root.toClassName()

        val rootScope = roundEnvironment.generateRootScope(root, rootClassName)
        val scopesCores = roundEnvironment.generateScopesBy(
            coreMarker = LifecycleScopeCore::class,
            scopeLevelMarker = LifecycleScope::class,
            rootClassName = rootClassName,
            rootDependencies = rootScope.providedDependencies
        )

        DakkerBuilder(root.toClassName(), scopesCores).build().write()
        return true
    }

    private fun RoundEnvironment.getRoot(): Element? {
        val annotatedRoots = getElementsAnnotatedWith(DakkerApplication::class.java) ?: emptySet()

        return when {
            annotatedRoots.isEmpty() -> null
            annotatedRoots.count() > 1 -> throw MoreThanOneInjectionRootException()
            else -> annotatedRoots.first()
        }
    }

    private fun RoundEnvironment.generateRootScope(
        root: Element,
        rootClassName: ClassName
    ): Scope {
        val rootLevelDependencies = getScopeLevelDependenciesSet(ApplicationScope::class, rootClassName)

        return mapOf(
            rootClassName to ScopeCore(
                rootClassName,
                null,
                (root as Symbol.ClassSymbol).getRequestedDependencies()
            )
        )
            .buildGraph(
                rootClassName = rootClassName,
                coreClass = rootClassName,
                scopeLevelDependencies = rootLevelDependencies,
                parentClassName = null,
                parentDependencies = emptySet()
            )
            .let { Scope(rootClassName, it) }
    }

    private fun RoundEnvironment.generateScopesBy(
        coreMarker: KClass<out Annotation>,
        scopeLevelMarker: KClass<out Annotation>,
        rootClassName: ClassName,
        rootDependencies: Set<Dependency>
    ): Set<ClassName> {
        val scopeLevelDependencies = getScopeLevelDependenciesSet(scopeLevelMarker, rootClassName)

        val cores = (getElementsAnnotatedWith(coreMarker.java) ?: emptySet())
            .asSequence()
            // Core of scope must be class
            .map { it.toClassSymbol() ?: throw IllegalAnnotationUsageException(coreMarker) }
            // Core of scope must implement LifecycleOwner, to Dakker can trash all scope onDestroy event
            .onEach { it.checkOnLifecycleOwnerInterface() }
            .map {
                val coreClassName = it.toClassName()
                coreClassName to ScopeCore(
                    coreClassName,
                    it.parentScopeCoreClass() ?: rootClassName,
                    it.getRequestedDependencies()
                )
            }
            .toMap()

        cores.filter { it.value.parentScopeCoreClass == rootClassName }
            .keys
            .forEach {
                cores.buildGraph(
                    coreClass = it,
                    rootClassName = rootClassName,
                    scopeLevelDependencies = scopeLevelDependencies,
                    parentClassName = rootClassName,
                    parentDependencies = rootDependencies
                )
            }
        return cores.keys
    }

    private fun RoundEnvironment.getScopeLevelDependenciesSet(
        scopeLevelMarker: KClass<out Annotation>,
        rootClassName: ClassName
    ): ScopeLevelDependencies {
        val scopeLevelDependencies = mutableSetOf<Pair<ClassName, Dependency>>()
        val scopeLevelDependenciesWithoutProviders = mutableSetOf<Pair<ClassName, Dependency>>()

        getElementsAnnotatedWith(scopeLevelMarker.java)?.forEach { element ->
            if (element !is Symbol) return@forEach

            val dependencyInfo = element.getDependencyInfo()
            val scopeCoreClass = dependencyInfo.scopeCoreClass ?: rootClassName
            val isSinglePerScope = dependencyInfo.isSinglePerScope
            when (element) {
                is Symbol.MethodSymbol ->
                    element
                        .asDependency(isSinglePerScope)
                        .let { scopeLevelDependencies.add(scopeCoreClass to it) }
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
                            if (count == 1) {
                                constructors
                                    .first()
                                    .asDependency(isSinglePerScope)
                                    .let { scopeLevelDependencies.add(scopeCoreClass to it) }
                                    .let { wasProviderAddedToCollection(it, element) }
                            } else {
                                element
                                    .asDependency(isSinglePerScope)
                                    .let { scopeLevelDependenciesWithoutProviders.add(scopeCoreClass to it) }
                                    .let { wasProviderAddedToCollection(it, element) }
                            }
                        }
            }
        }

        return ScopeLevelDependencies(
            scopeLevelDependencies.toGroupedMap(),
            scopeLevelDependenciesWithoutProviders.toGroupedMap()
        )
    }

    private fun Map<ClassName, ScopeCore>.buildGraph(
        coreClass: ClassName,
        rootClassName: ClassName,
        scopeLevelDependencies: ScopeLevelDependencies,
        parentClassName: ClassName?,
        parentDependencies: Set<Dependency>
    ): Set<Dependency> {
        val children = filter { it.value.parentScopeCoreClass == coreClass }
        val coreScope = get(coreClass) ?: throw RuntimeException("Something went wrong")

        val scopeDependencies = scopeLevelDependencies.withProviders[coreClass] ?: emptySet()
        val scopeDependenciesWithoutProviders = scopeLevelDependencies.withoutProviders[coreClass] ?: emptySet()
        val requestedDependencies = coreScope.requestedDependencies

        scopeDependencies
            .groupingBy { it.qualifiedName }
            .eachCount()
            .filter { it.value > 1 }
            .run {
                if (isNotEmpty()) {
                    throw DependenciesConflictException(keys.joinToString())
                }
            }

        val requestedAsParamsDependencies = scopeDependencies
            .asSequence()
            .map { it.params ?: emptyList() }
            .flatten()
            .filter {
                !requestedDependencies.contains(it) &&
                        !parentDependencies.contains(it) &&
                        !scopeDependenciesWithoutProviders.contains(it)
            }
            .toSet()

        val thisScopeDependencies: Set<Dependency> = coreScope.requestedDependencies
            .union(scopeDependenciesWithoutProviders)
            .union(scopeDependencies)
            .union(parentDependencies)
            .union(requestedAsParamsDependencies)

        val dependenciesWithoutProviders: Set<Dependency> = coreScope.requestedDependencies
            .union(scopeDependenciesWithoutProviders)
            .subtract(scopeDependencies)
            .subtract(parentDependencies)
            .union(requestedAsParamsDependencies)

        NodeBuilder(
            coreClassName = coreClass,
            parentCoreClassName = parentClassName,
            rootClassName = rootClassName,
            allDependencies = thisScopeDependencies,
            parentDependencies = parentDependencies,
            dependenciesWithoutProviders = dependenciesWithoutProviders,
            scopeDependencies = scopeDependencies,
            requestedDependencies = requestedDependencies
        ).build().write()

        children.forEach {
            buildGraph(
                coreClass = it.key,
                rootClassName = rootClassName,
                scopeLevelDependencies = scopeLevelDependencies,
                parentClassName = coreClass,
                parentDependencies = thisScopeDependencies
            )
        }
        return thisScopeDependencies
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

    private fun Symbol.ClassSymbol.asDependency(isSinglePerScope: Boolean) = asDependency(null, isSinglePerScope)

    private fun Symbol.MethodSymbol.asDependency(isSinglePerScope: Boolean) =
        asDependency(paramsAsDependencies(), isSinglePerScope)

    private fun Symbol.asDependency(params: List<Dependency>?, isSinglePerScope: Boolean) =
        Dependency(
            processingEnv.elementUtils.getPackageOf(this).toString(),
            enclClass().simpleName.toString(),
            isSinglePerScope,
            params
        )

    private fun Symbol.getDependencyInfo(): DependencyInfo {
        var coreClassName: ClassName? = null
        var isSinglePerScope: Boolean? = null
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "coreClass" -> coreClassName = (pair.snd.value as? Type.ClassType)?.toKClassList()
                    "isSinglePerScope" -> isSinglePerScope = (pair.snd.value as? Boolean)
                        ?: throw RuntimeException("Incorrect value used as isSinglePerScope")
                }
            }
        }
        return DependencyInfo(coreClassName, isSinglePerScope ?: true)
    }

    private fun Symbol.parentScopeCoreClass(): ClassName? {
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "parentScopeCoreClass" -> return (pair.snd.value as? Type.ClassType)?.toKClassList()
                }
            }
        }
        return null
    }

    private fun Symbol.ClassSymbol.checkOnLifecycleOwnerInterface() {
        if (!implementsLifecycleOwner()) throw IncorrectCoreOfScopeException(className())
    }

    private fun Symbol.ClassSymbol.implementsLifecycleOwner(): Boolean {
        if (interfaces.nonEmpty()) {
            interfaces.find { it.isKindOfLifecycleOwner() }?.run { return true }
        }

        if (superclass == Type.noType) return false

        return (superclass.tsym as? Symbol.ClassSymbol)?.implementsLifecycleOwner() ?: false
    }

    private fun Type.isKindOfLifecycleOwner(): Boolean {
        if (this !is Type.ClassType) return false

        if (toKClassList() == LifecycleOwner::class.asClassName()) return true

        return interfaces_field?.find { isKindOfLifecycleOwner() } != null
    }

    private fun Element.toClassSymbol(): Symbol.ClassSymbol? {
        return if (this is Symbol.ClassSymbol) this else null
    }

    private fun Element.toClassName() = ClassName(
        processingEnv.elementUtils.getPackageOf(this).toString(),
        simpleName.toString()
    )

    private fun Set<Pair<ClassName, Dependency>>.toGroupedMap() = asSequence()
        .groupBy(Pair<ClassName, Dependency>::first) { it.second }
        .mapValues { it.value.toSet() }

    override fun hashCode(): Int {
        return 1
    }
}
