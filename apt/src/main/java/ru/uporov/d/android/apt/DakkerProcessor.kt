package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import ru.uporov.d.android.apt.builder.DakkerBuilder
import ru.uporov.d.android.apt.builder.ModuleBuilder
import ru.uporov.d.android.apt.model.*
import ru.uporov.d.android.common.APPLICATION_SCOPE_ID
import ru.uporov.d.android.common.annotation.*
import ru.uporov.d.android.common.exception.*
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
            coreMarker = DakkerScopeCore::class,
            scopeLevelMarker = DakkerScope::class,
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
        val rootLevelDependencies = getScopeLevelDependencies(ApplicationScope::class)

        return mapOf(
            APPLICATION_SCOPE_ID to ScopeCore(
                APPLICATION_SCOPE_ID,
                null,
                rootClassName,
                (root as Symbol.ClassSymbol).getRequestedDependencies()
            )
        )
            .buildGraph(
                scopeId = APPLICATION_SCOPE_ID,
                parentScopeId = null,
                rootClassName = rootClassName,
                coreClass = rootClassName,
                scopeLevelDependencies = rootLevelDependencies,
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
        val scopeLevelDependencies = getScopeLevelDependencies(scopeLevelMarker)

        (getElementsAnnotatedWith(coreMarker.java) ?: emptySet())
            .asSequence()
            // Core of scope must be class
            .map { it.toClassSymbol() ?: throw IllegalAnnotationUsageException(coreMarker) }
            // Core of scope must implement Destroyable or LifecycleOwner, to Dakker can trash all scope onDestroy event
            .onEach(Symbol.ClassSymbol::checkOnDestroyable)
            .map {
                val coreClassName = it.toClassName()
                val (scopeId, parentScopeId) = it.getScopeCoreInfo()
                val requestedDependencies = it.getRequestedDependencies()

                ScopeCore(
                    scopeId,
                    parentScopeId,
                    coreClassName,
                    requestedDependencies
                )
            }
            .also {
                val cores = it.map { core -> core.scopeId to core }.toMap()
                if (it.count() > cores.count()) {
                    val ids = it.map { core -> core.scopeId }.toMutableList()
                    cores.keys.forEach { id ->
                        ids.remove(id)
                    }
                    throw SeveralScopesUseTheSameIdException(ids.first())
                }
                cores.asSequence()
                    .filter { entry -> entry.value.parentScopeId == APPLICATION_SCOPE_ID }
                    .forEach { (scopeId, core) ->
                        cores.buildGraph(
                            scopeId = scopeId,
                            parentScopeId = core.parentScopeId,
                            coreClass = core.coreClass,
                            rootClassName = rootClassName,
                            scopeLevelDependencies = scopeLevelDependencies,
                            parentDependencies = rootDependencies
                        )
                    }
            }
            .map(ScopeCore::coreClass)
            .toSet()
            .run { return this }
    }

    private fun RoundEnvironment.getScopeLevelDependencies(scopeLevelMarker: KClass<out Annotation>): ScopeLevelDependencies {
        val scopeLevelDependencies = mutableSetOf<Pair<Int, Dependency>>()
        val scopeLevelDependenciesWithoutProviders = mutableSetOf<Pair<Int, Dependency>>()

        getElementsAnnotatedWith(scopeLevelMarker.java)?.forEach { element ->
            if (element !is Symbol) return@forEach

            val (scopeId, isSinglePerScope) = element.getDependencyInfo()

            when (element) {
                is Symbol.MethodSymbol -> element
                    .asDependency(isSinglePerScope)
                    .let { scopeLevelDependencies.add(scopeId to it) }
                    .let { wasProviderAddedToCollection(it, element.enclClass()) }
                is Symbol.ClassSymbol -> element
                    .asDependency(isSinglePerScope)
                    .let { scopeLevelDependenciesWithoutProviders.add(scopeId to it) }
                    .let { wasProviderAddedToCollection(it, element) }
            }
        }

        return ScopeLevelDependencies(
            scopeLevelDependencies.toGroupedMap(),
            scopeLevelDependenciesWithoutProviders.toGroupedMap()
        )
    }

    private fun Map<Int, ScopeCore>.buildGraph(
        scopeId: Int,
        parentScopeId: Int? = null,
        coreClass: ClassName,
        rootClassName: ClassName,
        scopeLevelDependencies: ScopeLevelDependencies,
        parentDependencies: Set<Dependency>
    ): Set<Dependency> {
        val children = filter { it.value.parentScopeId == scopeId }
        val coreScope = get(scopeId) ?: throw RuntimeException("Something went wrong")

        val scopeDependencies = scopeLevelDependencies.withProviders[scopeId] ?: emptySet()
        val scopeDependenciesWithoutProviders = scopeLevelDependencies.withoutProviders[scopeId] ?: emptySet()
        val requestedDependencies = coreScope.requestedDependencies

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

        ModuleBuilder(
            coreClassName = coreClass,
            parentCoreClassName = get(parentScopeId)?.coreClass ?: rootClassName,
            rootClassName = rootClassName,
            allDependencies = thisScopeDependencies,
            parentDependencies = parentDependencies,
            dependenciesWithoutProviders = dependenciesWithoutProviders,
            scopeDependencies = scopeDependencies,
            requestedDependencies = requestedDependencies
        ).build().write()

        children.forEach { (scopeId, core) ->
            buildGraph(
                scopeId = scopeId,
                parentScopeId = core.parentScopeId,
                coreClass = core.coreClass,
                rootClassName = rootClassName,
                scopeLevelDependencies = scopeLevelDependencies,
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
            .map {
                Dependency(it.type.returnType.asTypeName().javaToKotlinType())
            }
            .toSet()
    }

    private fun FileSpec.write() = writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))

    private fun Symbol.getDependencyInfo(): DependencyInfo {
        var scopeId: Int? = null
        var isSinglePerScope: Boolean? = null
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "scopeId" -> scopeId = pair.snd.value as? Int
                    "isSinglePerScope" -> isSinglePerScope = (pair.snd.value as? Boolean)
                        ?: throw RuntimeException("Incorrect value used as isSinglePerScope")
                }
            }
        }
        return DependencyInfo(
            scopeId ?: APPLICATION_SCOPE_ID,
            isSinglePerScope ?: true
        )
    }

    private fun Symbol.getScopeCoreInfo(): ScopeCoreInfo {
        var scopeId: Int? = null
        var parentScopeId: Int? = null
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "scopeId" -> scopeId = pair.snd.value as? Int
                    "parentScopeId" -> parentScopeId = pair.snd.value as? Int
                }
            }
        }
        scopeId ?: throw NoScopeIdException(qualifiedName.toString())
        if (scopeId == APPLICATION_SCOPE_ID) throw ApplicationScopeIdUsageException(scopeId, qualifiedName.toString())

        return ScopeCoreInfo(scopeId, parentScopeId ?: APPLICATION_SCOPE_ID)
    }

    override fun hashCode(): Int {
        return 1
    }
}
