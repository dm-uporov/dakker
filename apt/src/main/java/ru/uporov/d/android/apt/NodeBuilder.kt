package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import ru.uporov.d.android.common.exception.DependenciesConflictException
import ru.uporov.d.android.common.exception.DependencyIsNotProvidedException
import ru.uporov.d.android.common.provider.Provider

private const val FILE_NAME_FORMAT = "Dakker%s"
private const val PROVIDER_NAME_FORMAT = "%sProvider"

private const val DAKKER_GET_NODE_FORMAT = "Dakker.get%s()"

class NodeBuilder(
    private val coreClassName: ClassName,
    private val rootClassName: ClassName,
    private val rootDependencies: Set<Dependency>,
    private val scopeDependencies: Set<Dependency>,
    private val scopeDependenciesWithoutProviders: Set<Dependency>,
    private val requestedDependencies: Set<Dependency>
) {

    private val pack: String = coreClassName.packageName
    private val coreName: String = coreClassName.simpleName

    private val fileName = FILE_NAME_FORMAT.format(coreName)
    private val nodeName = coreClassName.nodeName()

    private val nodeClassName = ClassName(pack, nodeName)
    private val coreNodeFromDakkerStatement = DAKKER_GET_NODE_FORMAT.format(nodeName)
    private val rootNodeFromDakkerStatement = DAKKER_GET_NODE_FORMAT.format(rootClassName.nodeName())

    private val providedByRootDependencies: Set<Dependency> = rootDependencies.intersect(
        requestedDependencies.union(
            scopeDependencies.map { it.params ?: emptyList() }.flatten().toSet()
        )
    )
    private val allDependencies: Set<Dependency> = requestedDependencies
        .union(scopeDependenciesWithoutProviders)
        .union(scopeDependencies)
        .union(providedByRootDependencies)
    private val dependenciesWithoutProviders: Set<Dependency> = requestedDependencies
        .union(scopeDependenciesWithoutProviders)
        .subtract(scopeDependencies)
        .subtract(rootDependencies)

    fun build(): FileSpec {
        checkDependenciesGraph()
        return FileSpec.builder(pack, fileName)
            .addImport("ru.uporov.d.android.common.provider", "single", "factory")
            .withInjectFunctions()
            .withGetFunctions()
            .withNodeClass()
            .build()
    }

    // TODO надо бы уточнять, в каком конкретно скоупе проблема
    private fun checkDependenciesGraph() {
        // check on existence every providers
        scopeDependencies
            .asSequence()
            .map { it.params ?: emptyList() }
            .flatten()
            .forEach {
                if (!allDependencies.contains(it) && !rootDependencies.contains(it)) {
                    throw DependencyIsNotProvidedException(it.qualifiedName)
                }
            }

        // Check on conflicting providers (if there is more than one scope providers for one type)
        scopeDependencies
            .groupingBy { it.qualifiedName }
            .eachCount()
            .filter { it.value > 1 }
            .run {
                if (isNotEmpty()) {
                    throw DependenciesConflictException(keys.joinToString())
                }
            }

        // TODO check on graph conflicts
    }

    private fun FileSpec.Builder.withInjectFunctions() = apply {
        requestedDependencies.forEach {
            addFunction(
                FunSpec.builder("inject${it.name}")
                    .receiver(coreClassName)
                    .returns(Lazy::class.asClassName().parameterizedBy(it.className))
                    .addStatement(" return lazy { $coreNodeFromDakkerStatement.${it.name.asProviderParamName()}.invoke(this) }")
                    .build()
            )
        }
    }

    private fun FileSpec.Builder.withGetFunctions() = apply {
        allDependencies.forEach {
            addFunction(
                FunSpec.builder("get${it.name.capitalize()}")
                    .receiver(coreClassName)
                    .returns(ClassName.bestGuess(it.qualifiedName))
                    .addStatement(" return $coreNodeFromDakkerStatement.${it.name.asProviderParamName()}.invoke(this)")
                    .build()
            )
        }
    }

    private fun FileSpec.Builder.withNodeClass() = apply {
        addType(
            TypeSpec.classBuilder(nodeName)
                .withNodeConstructor()
                .withNodeCompanion()
                .withProvidersProperties()
                .withTrashFunction()
                .build()
        )
    }

    private fun TypeSpec.Builder.withNodeConstructor() = apply {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .withProvidersLambdasParamsOf(allDependencies)
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
    }

    private fun TypeSpec.Builder.withNodeCompanion() = apply {
        addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(
                    FunSpec.builder(nodeName.decapitalize())
                        .receiver(rootClassName)
                        .returns(nodeClassName)
                        .withProvidersLambdasParamsOf(dependenciesWithoutProviders)
                        .addStatement("""
                            return $nodeName(
                        ${dependenciesWithoutProviders.joinToString {
                            val name = it.name.asProviderParamName()
                            return@joinToString "$name = $name"
                        }
                        }
                        ${if (scopeDependencies.isEmpty() || dependenciesWithoutProviders.isEmpty()) "" else ","}
                        ${scopeDependencies.joinToString(",\n") { element ->
                            "${element.name.asProviderParamName()} = " +
                                    "${if (element.isSinglePerScope) "single" else "factory"} {\n" +
                                    "${element.name}(" +
                                    (element.params?.joinToString {
                                        "$coreNodeFromDakkerStatement.${it.name.asProviderParamName()}.invoke(it)"
                                    } ?: "") +
                                    ")" +
                                    "\n}"
                        }}
                        ${if (providedByRootDependencies.isEmpty()) "" else ","}
                            ${providedByRootDependencies.joinToString(",\n") { element ->
                            val providerName = element.name.asProviderParamName()
                            "$providerName = factory { $rootNodeFromDakkerStatement.$providerName.invoke(this) }"
                        }}
                            )
                        """.trimIndent()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.withProvidersProperties() = apply {
        allDependencies
            .map {
                PropertySpec.builder(
                    it.name.asProviderParamName(),
                    Provider::class.asClassName().parameterizedBy(coreClassName, it.className)
                )
                    .initializer(it.name.asProviderParamName())
                    .build()
            }.let(::addProperties)
    }

    private fun TypeSpec.Builder.withTrashFunction() = apply {
        addFunction(
            FunSpec.builder("trash")
                .apply {
                    allDependencies.forEach {
                        addStatement("${it.name.asProviderParamName()}.trashValue()")
                    }
                }
                .build()
        )
    }

    private fun FunSpec.Builder.withProvidersLambdasParamsOf(dependencies: Set<Dependency>) = apply {
        dependencies.forEach {
            addParameter(
                ParameterSpec.builder(
                    it.name.asProviderParamName(),
                    Provider::class.asClassName().parameterizedBy(coreClassName, it.className)
                ).build()
            )
        }
    }

    private fun String.asProviderParamName() = PROVIDER_NAME_FORMAT.format(this).decapitalize()
}