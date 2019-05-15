package ru.uporov.d.android.apt.builder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import ru.uporov.d.android.apt.model.Dependency
import ru.uporov.d.android.apt.nodeName
import ru.uporov.d.android.common.provider.Provider

private const val FILE_NAME_FORMAT = "$DAKKER_FILE_NAME%s"
private const val PROVIDER_NAME_FORMAT = "%sProvider"

private const val DAKKER_GET_NODE_FORMAT = "$DAKKER_FILE_NAME.get%s()"

class NodeBuilder(
    private val coreClassName: ClassName,
    private val parentCoreClassName: ClassName?,
    private val rootClassName: ClassName,
    private val allDependencies: Set<Dependency>,
    private val parentDependencies: Set<Dependency>,
    private val dependenciesWithoutProviders: Set<Dependency>,
    private val scopeDependencies: Set<Dependency>,
    private val requestedDependencies: Set<Dependency>
) {

    private val pack: String = coreClassName.packageName
    private val coreName: String = coreClassName.simpleName

    private val fileName = FILE_NAME_FORMAT.format(coreName)
    private val nodeName = coreClassName.nodeName()

    private val nodeClassName = ClassName(pack, nodeName)
    private val coreNodeFromDakkerStatement = DAKKER_GET_NODE_FORMAT.format(nodeName)
    private val parentCoreNodeFromDakkerStatement = DAKKER_GET_NODE_FORMAT.format(parentCoreClassName?.nodeName())
    private val rootCoreNodeFromDakkerStatement = DAKKER_GET_NODE_FORMAT.format(rootClassName.nodeName())

    fun build(): FileSpec {
        return FileSpec.builder(pack, fileName)
            .addImport(rootClassName.packageName, DAKKER_FILE_NAME)
            .addImport("ru.uporov.d.android.common.provider", "single", "factory")
            .withInjectFunctions()
            .withGetFunctions()
            .withNodeClass()
            .build()
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
                        .withParentCoreProviderParam()
                        .withProvidersLambdasParamsOf(dependenciesWithoutProviders)
                        .addStatement("""
                            return $nodeName(
                        ${dependenciesWithoutProviders.joinToString {
                            val name = it.name.asProviderParamName()
                            return@joinToString "$name = $name"
                        }
                        }
                        ${if (dependenciesWithoutProviders.isEmpty() || scopeDependencies.isEmpty()) "" else ","}
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
                        ${if ((dependenciesWithoutProviders.isEmpty() && scopeDependencies.isEmpty()) || parentDependencies.isEmpty()) "" else ","}
                            ${parentDependencies.joinToString(",\n") {
                            val providerName = it.name.asProviderParamName()
                            "$providerName = factory { " +
                                    if (parentCoreClassName == rootClassName) {
                                        "$rootCoreNodeFromDakkerStatement.$providerName.invoke(this)"
                                    } else {
                                        "$parentCoreNodeFromDakkerStatement.$providerName.invoke(it.parentCoreProvider()) "
                                    } +
                                    "}"
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

    private fun FunSpec.Builder.withParentCoreProviderParam() = apply {
        parentCoreClassName?.let { parentCoreClassName ->
            if (parentCoreClassName != rootClassName) {
                addParameter(
                    ParameterSpec.builder(
                        "parentCoreProvider",
                        LambdaTypeName.get(receiver = coreClassName, returnType = parentCoreClassName)
                    ).build()
                )
            }
        }
    }

    private fun String.asProviderParamName() = PROVIDER_NAME_FORMAT.format(this).decapitalize()
}