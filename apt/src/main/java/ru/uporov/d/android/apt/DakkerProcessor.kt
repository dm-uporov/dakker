package ru.uporov.d.android.apt

import androidx.lifecycle.LifecycleOwner
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
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

        val appScope = roundEnvironment.generateScopesBy(
            coreMarker = DakkerApplication::class,
            scopeLevelMarker = ApplicationScope::class,
            root = root,
            parentScopes = emptySet(),
            isRootScope = true
        )

        val activityScopes = appScope.union(
            roundEnvironment.generateScopesBy(
                coreMarker = DakkerActivity::class,
                scopeLevelMarker = ActivityScope::class,
                root = root,
                parentScopes = appScope,
                isRootScope = false
            )
        )

        val fragmentScopes = activityScopes.union(
            roundEnvironment.generateScopesBy(
                coreMarker = DakkerFragment::class,
                scopeLevelMarker = FragmentScope::class,
                root = root,
                parentScopes = activityScopes,
                isRootScope = false
            )
        )

        DakkerBuilder(
            root.toClassName(),
            fragmentScopes.subtract(appScope).asSequence().map { it.core }.toSet()
        ).build().write()
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

    private fun RoundEnvironment.generateScopesBy(
        coreMarker: KClass<out Annotation>,
        scopeLevelMarker: KClass<out Annotation>,
        root: Element,
        parentScopes: Set<Scope>,
        isRootScope: Boolean
    ): Set<Scope> {
        val rootClassName = root.toClassName()
        val scopeDependencies = mutableSetOf<Pair<ClassName?, Dependency>>()
        val scopeDependenciesWithoutProviders = mutableSetOf<Pair<ClassName?, Dependency>>()

        getElementsAnnotatedWith(scopeLevelMarker.java)?.forEach { element ->
            if (element !is Symbol) return@forEach

            val className = element.getCoreClassNameOrNull()
            val isSinglePerScope = element.getIsSinglePerScope()
            when (element) {
                is Symbol.MethodSymbol ->
                    element
                        .asDependency(isSinglePerScope)
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
                                    .asDependency(isSinglePerScope)
                                    .let { scopeDependenciesWithoutProviders.add(className to it) }
                                    .let { wasProviderAddedToCollection(it, element) }
                            } else if (count == 1) {
                                constructors
                                    .first()
                                    .asDependency(isSinglePerScope)
                                    .let { scopeDependencies.add(className to it) }
                                    .let { wasProviderAddedToCollection(it, element) }
                            }
                        }
            }
        }

        val scopeDependenciesMap = scopeDependencies
            .asSequence()
            .groupBy { it.first }
            .mapKeys { it.key ?: rootClassName }
            .mapValues { it.value.map { pair -> pair.second }.toSet() }

        val scopeDependenciesWithoutProvidersMap = scopeDependenciesWithoutProviders
            .asSequence()
            .groupBy { it.first }
            .mapKeys { it.key ?: rootClassName }
            .mapValues { it.value.map { pair -> pair.second }.toSet() }

        val requestedDependenciesMap = mutableMapOf<ClassName, Set<Dependency>>()

        if (isRootScope) {
            setOf(root)
        } else {
            getElementsAnnotatedWith(coreMarker.java) ?: emptySet()
        }
            .asSequence()
            // Core of scope must be class
            .map { it.toClassSymbol() ?: throw IllegalAnnotationUsageException(coreMarker) }
            // Core of scope must implement LifecycleOwner, to Dakker can trash all scope onDestroy
            .onEach { if (!isRootScope) it.checkOnLifecycleOwnerInterface() }
            .map {
                return@map it.toClassName().also { name ->
                    requestedDependenciesMap[name] = it.getRequestedDependencies()
                }
            }
            .toSet()
            .union(scopeDependenciesMap.keys)
            .union(scopeDependenciesWithoutProvidersMap.keys)
            .map {
                val thisScopeDependencies = scopeDependenciesMap[it] ?: emptySet()
                val thisScopeDependenciesWithoutProviders = scopeDependenciesWithoutProvidersMap[it] ?: emptySet()
                val requestedDependencies = requestedDependenciesMap[it] ?: emptySet()
                NodeBuilder(
                    coreClassName = it,
                    rootClassName = rootClassName,
                    parentScopes = parentScopes,
                    scopeDependencies = thisScopeDependencies,
                    scopeDependenciesWithoutProviders = thisScopeDependenciesWithoutProviders,
                    requestedDependencies = requestedDependencies
                )
                    .build()
                    .write()
                return@map Scope(
                    it,
                    thisScopeDependenciesWithoutProviders
                        .union(thisScopeDependencies)
                        .union(requestedDependencies)
                )
            }
            .toSet()
            .run { return this }
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

    private fun Symbol.ClassSymbol.asDependency(isSinglePerScope: Boolean?) = asDependency(null, isSinglePerScope)

    private fun Symbol.MethodSymbol.asDependency(isSinglePerScope: Boolean?) =
        asDependency(paramsAsDependencies(), isSinglePerScope)

    private fun Symbol.asDependency(params: List<Dependency>?, isSinglePerScope: Boolean?) =
        Dependency(
            processingEnv.elementUtils.getPackageOf(this).toString(),
            enclClass().simpleName.toString(),
            isSinglePerScope ?: true,
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

    private fun Symbol.getIsSinglePerScope(): Boolean? {
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "isSinglePerScope" -> return (pair.snd.value as? Boolean)
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

        interfaces_field?.find { isKindOfLifecycleOwner() }?.run {
            return true
        } ?: return false
    }

    private fun Element.toClassSymbol(): Symbol.ClassSymbol? {
        return if (this is Symbol.ClassSymbol) this else null
    }

    private fun Element.toClassName() = ClassName(
        processingEnv.elementUtils.getPackageOf(this).toString(),
        simpleName.toString()
    )

    override fun hashCode(): Int {
        return 1
    }
}
