package ru.uporov.d.android.apt.builder

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import ru.uporov.d.android.apt.model.Dependency
import ru.uporov.d.android.apt.moduleName
import ru.uporov.d.android.common.provider.Provider
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

private const val FILE_NAME_FORMAT = "$DAKKER_FILE_NAME%s"
private const val PROVIDER_NAME_FORMAT = "%sProvider"

private const val DAKKER_GET_MODULE_FORMAT = "$DAKKER_FILE_NAME.get%s()"

class ModuleBuilder(
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
    private val moduleName = coreClassName.moduleName()

    private val moduleClassName = ClassName(pack, moduleName)
    private val coreModuleFromDakkerStatement = DAKKER_GET_MODULE_FORMAT.format(moduleName)
    private val parentCoreModuleFromDakkerStatement = DAKKER_GET_MODULE_FORMAT.format(parentCoreClassName?.moduleName())
    private val rootCoreModuleFromDakkerStatement = DAKKER_GET_MODULE_FORMAT.format(rootClassName.moduleName())

    fun build(): FileSpec {
        return FileSpec.builder(pack, fileName)
            .addImport(rootClassName.packageName, DAKKER_FILE_NAME)
            .addImport("ru.uporov.d.android.common.provider", "single", "factory")
            .withInjectFunctions()
            .withGetFunctions()
            .withModuleClass()
            .build()
    }

    private fun FileSpec.Builder.withInjectFunctions() = apply {
        requestedDependencies.forEach {

            val lazy = JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(FqName(it.typeName.toString()))
                ?.asSingleFqName()
                ?.asString()
                ?.let {
                    Lazy::class.asClassName().parameterizedBy(ClassName.bestGuess(it))
                } ?: Lazy::class.asClassName().parameterizedBy(it.typeName)

            addFunction(
                FunSpec.builder("inject${it.uniqueName}")
                    .receiver(coreClassName)
                    .returns(lazy)
                    .addStatement(" return lazy { $coreModuleFromDakkerStatement.${it.uniqueName.asProviderParamName()}.invoke(this) }")
                    .build()
            )
        }
    }

    private fun FileSpec.Builder.withGetFunctions() = apply {
        allDependencies.forEach {
            addFunction(
                FunSpec.builder("get${it.uniqueName}")
                    .receiver(coreClassName)
                    .returns(it.typeName)
                    .addStatement(" return $coreModuleFromDakkerStatement.${it.uniqueName.asProviderParamName()}.invoke(this)")
                    .build()
            )
        }
    }

    private fun FileSpec.Builder.withModuleClass() = apply {
        addType(
            TypeSpec.classBuilder(moduleName)
                .withModuleConstructor()
                .withModuleCompanion()
                .withProvidersProperties()
                .build()
        )
    }

    private fun TypeSpec.Builder.withModuleConstructor() = apply {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .withProvidersLambdasParamsOf(allDependencies)
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
    }

    private fun TypeSpec.Builder.withModuleCompanion() = apply {
        addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(
                    FunSpec.builder(moduleName.decapitalize())
                        .receiver(rootClassName)
                        .returns(moduleClassName)
                        .withParentCoreProviderParam()
                        .withProvidersLambdasParamsOf(dependenciesWithoutProviders)
                        .addStatement("""
                            return $moduleName(
                        ${dependenciesWithoutProviders.joinToString {
                            val name = it.uniqueName.asProviderParamName()
                            return@joinToString "$name = $name"
                        }
                        }
                        ${if (dependenciesWithoutProviders.isEmpty() || scopeDependencies.isEmpty()) "" else ","}
                        ${scopeDependencies.joinToString(",\n") { element ->
                            "${element.uniqueName.asProviderParamName()} = " +
                                    "${if (element.isSinglePerScope) "single" else "factory"} {\n" +
                                    "${element.uniqueName}(" +
                                    (element.params?.joinToString {
                                        "$coreModuleFromDakkerStatement.${it.uniqueName.asProviderParamName()}.invoke(it)"
                                    } ?: "") +
                                    ")" +
                                    "\n}"
                        }}
                        ${if ((dependenciesWithoutProviders.isEmpty() && scopeDependencies.isEmpty()) || parentDependencies.isEmpty()) "" else ","}
                            ${parentDependencies.joinToString(",\n") {
                            val providerName = it.uniqueName.asProviderParamName()
                            "$providerName = factory { " +
                                    if (parentCoreClassName == rootClassName) {
                                        "$rootCoreModuleFromDakkerStatement.$providerName.invoke(this)"
                                    } else {
                                        "$parentCoreModuleFromDakkerStatement.$providerName.invoke(it.parentCoreProvider()) "
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
                    it.uniqueName.asProviderParamName(),
                    Provider::class.asClassName().parameterizedBy(coreClassName, it.typeName)
                )
                    .initializer(it.uniqueName.asProviderParamName())
                    .build()
            }.let(::addProperties)
    }

    private fun FunSpec.Builder.withProvidersLambdasParamsOf(dependencies: Set<Dependency>) = apply {
        dependencies.forEach {
            addParameter(
                ParameterSpec.builder(
                    it.uniqueName.asProviderParamName(),
                    Provider::class.asClassName().parameterizedBy(coreClassName, it.typeName)
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