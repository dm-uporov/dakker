package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import ru.uporov.d.android.common.annotation.*
import ru.uporov.d.android.common.exception.DependenciesConflictException
import ru.uporov.d.android.common.exception.IllegalAnnotationUsageException
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
            InjectionRoot::class.java.name,
            ApplicationScope::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        val root = roundEnvironment.getRoot() ?: return true

        val appScopeLevel = roundEnvironment.generateScopesBy(
            coreMarker = InjectionRoot::class,
            scopeLevelMarker = ApplicationScope::class,
            root = root,
            providedByRootDependencies = emptySet(),
            isRootScope = true
        )
        val activityScopeLevel = roundEnvironment.generateScopesBy(
            coreMarker = InjectionNode::class,
            scopeLevelMarker = NodeScope::class,
            root = root,
            providedByRootDependencies = appScopeLevel.providedDependencies,
            isRootScope = false
        )

        DakkerBuilder(root.toClassName(), activityScopeLevel.nodes).build().write()
        return true
    }

    private fun RoundEnvironment.getRoot(): Element? {
        val annotatedRoots = getElementsAnnotatedWith(InjectionRoot::class.java) ?: emptySet()

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
        providedByRootDependencies: Set<Dependency>,
        isRootScope: Boolean
    ): ScopeLevel {
        val rootClassName = root.toClassName()
        val scopeDependencies = mutableSetOf<Pair<ClassName?, Dependency>>()
        val scopeDependenciesWithoutProviders = mutableSetOf<Pair<ClassName?, Dependency>>()

        getElementsAnnotatedWith(scopeLevelMarker.java)?.forEach { element ->
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
            .map { it.toClassSymbol() ?: throw IllegalAnnotationUsageException(coreMarker) }
            .map {
                return@map it.toClassName().also { name ->
                    requestedDependenciesMap[name] = it.getRequestedDependencies()
                }
            }
            .toSet()
            .union(scopeDependenciesMap.keys)
            .union(scopeDependenciesWithoutProvidersMap.keys)
            .onEach {
                NodeBuilder(
                    coreClassName = it,
                    rootClassName = rootClassName,
                    rootDependencies = providedByRootDependencies,
                    scopeDependencies = scopeDependenciesMap[it] ?: emptySet(),
                    scopeDependenciesWithoutProviders = scopeDependenciesWithoutProvidersMap[it] ?: emptySet(),
                    requestedDependencies = requestedDependenciesMap[it] ?: emptySet()
                )
                    .build()
                    .write()
            }
            .toSet()
            .let {
                ScopeLevel(
                    it.map(ClassName::nodeClassName).toSet(),
                    scopeDependenciesMap.values.flatten()
                        .union(scopeDependenciesWithoutProvidersMap.values.flatten())
                        .union(requestedDependenciesMap.values.flatten())
                )
            }
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
