package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import ru.uporov.d.android.common.DakkerBean
import ru.uporov.d.android.common.exception.DependenciesConflictException
import ru.uporov.d.android.common.exception.DependenciesCycleException
import ru.uporov.d.android.common.exception.DependencyIsNotProvidedException
import kotlin.reflect.KClass

private const val MODULE_NAME_FORMAT = "Dakker%s"
private const val BEAN_NAME_FORMAT = "%sBean"
private const val PROVIDER_NAME_FORMAT = "%sProvider"

class BeanBuilder(
    private val pack: String,
    private val rootName: String,
    private val scopeDependencies: List<Dependency>,
    private val requestedDependencies: List<Dependency>
) {

    private val moduleName = MODULE_NAME_FORMAT.format(rootName)
    private val beanName = BEAN_NAME_FORMAT.format(rootName)

    private val rootClassName = ClassName.bestGuess("$pack.$rootName")
    private val beanClassName = ClassName("$pack.$moduleName", beanName)

    private val allDependencies: Set<Dependency> = requestedDependencies.union(scopeDependencies)
    private val dependenciesWithoutProviders: Set<Dependency> = requestedDependencies.subtract(scopeDependencies)

    fun build(): FileSpec {
        checkDependenciesGraph()
        return FileSpec.builder(pack, moduleName)
            .generateModule()
            .addFunction(
                FunSpec.builder("startDakkerModule")
                    .receiver(ClassName.bestGuess("androidx.lifecycle.LifecycleOwner"))
                    .addStatement(
                        """
                            // TODO create method like this for every InjectionBean
                            // and subscribe on lifecycle events.
                            // Destroy module on lifecycle Destroy event
                        """.trimIndent()
                    )
                    .build()
            )
            .build()
    }

    // TODO надо бы уточнять, в каком конкретно скоупе проблема
    private fun checkDependenciesGraph() {
        // check on existence every providers
        scopeDependencies
            .asSequence()
            .map { it.params ?: emptyList() }
            .flatten()
            .forEach { if (!allDependencies.contains(it)) throw DependencyIsNotProvidedException(it.qualifiedName) }


        // Check on conflicting providers
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

    private fun FileSpec.Builder.generateModule() = apply {
        addType(
            TypeSpec.objectBuilder(moduleName)
                .beanLateinitProperty()
                .startDakkerFunction()
                .parametrizedGetFunction()
                .injectFunctions()
                .getFunctions()
                .beanInnerClass()
                .build()
        )
    }

    private fun TypeSpec.Builder.beanLateinitProperty() = apply {
        addProperty(
            PropertySpec.builder(
                "root",
                beanClassName,
                KModifier.PRIVATE,
                KModifier.LATEINIT
            )
                .mutable(true)
                .build()
        )
    }

    private fun TypeSpec.Builder.startDakkerFunction() = apply {
        addFunction(
            FunSpec.builder("startDakker")
                .receiver(rootClassName)
                .addParameter("rootModule", beanClassName)
                .addStatement("root = rootModule")
                .build()
        )
    }

    private fun TypeSpec.Builder.parametrizedGetFunction() = apply {
        addFunction(
            FunSpec.builder("get")
                .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                .addTypeVariable(TypeVariableName("reified T"))
                .receiver(rootClassName)
                .returns(TypeVariableName("T"))
                .addStatement(
                    """
                    Thread.currentThread().stackTrace
                        .asSequence()
                        .groupBy { it }
                        .filter { it.value.size > 1 }
                        .run {
                            if (isNotEmpty()) {
                                throw ${DependenciesCycleException::class.qualifiedName}(T::class)
                            }
                        }
                    return root.providers[T::class]?.invoke(this@get) as T
                    """.trimIndent()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.injectFunctions(): TypeSpec.Builder = apply {
        requestedDependencies.forEach {
            addFunction(
                FunSpec.builder("inject${it.name}")
                    .receiver(rootClassName)
                    .returns(Lazy::class.asClassName().parameterizedBy(it.className))
                    .addStatement(" return lazy { get<${it.name}>() }")
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.getFunctions(): TypeSpec.Builder = apply {
        scopeDependencies.forEach {
            addFunction(
                FunSpec.builder("get${it.name.capitalize()}")
                    .receiver(rootClassName)
                    .returns(ClassName.bestGuess(it.qualifiedName))
                    .addStatement(" return get<${it.name}>()")
                    .build()
            )
        }
    }

    private fun TypeSpec.Builder.beanInnerClass(): TypeSpec.Builder = apply {
        addType(
            TypeSpec.classBuilder(beanName)
                .beanConstructor()
                .superclass(DakkerBean::class.asClassName().parameterizedBy(rootClassName))
                .beanCompanion()
                .beanClassProperty()
                .beanProvidersProperty()
                .build()
        )
    }

    private fun TypeSpec.Builder.beanConstructor() = apply {
        primaryConstructor(
            FunSpec.constructorBuilder()
                .withProvidersLambdasParamsOf(allDependencies)
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
    }

    private fun TypeSpec.Builder.beanCompanion() = apply {
        addType(
            TypeSpec.companionObjectBuilder()
                .addFunction(
                    FunSpec.builder(BEAN_NAME_FORMAT.format(rootName.decapitalize()))
                        .returns(ClassName("$pack.$moduleName", beanName))
                        .withProvidersLambdasParamsOf(dependenciesWithoutProviders)
                        .addStatement(
                            """ return $beanName(
                            ${dependenciesWithoutProviders.joinToString {
                                val name = it.name.asProviderParamName()
                                return@joinToString "$name = $name"
                            }
                            },
                            ${scopeDependencies.joinToString(",\n") { element ->
                                "${element.name.asProviderParamName()} = {\n" +
                                        "${element.name}(" +
                                        element.params?.joinToString { "it.get<${it.name}>()" } +
                                        ")" +
                                        "\n}"
                            }}
                            )
                            """.trimIndent()
                        )
                        .build()
                )
                .build()
        )
    }

    private fun TypeSpec.Builder.beanClassProperty() = apply {
        addProperty(
            PropertySpec
                .builder("beanClass", KClass::class.asClassName().parameterizedBy(rootClassName))
                .initializer("$rootName::class")
                .addModifiers(KModifier.OVERRIDE)
                .build()
        )
    }

    private fun TypeSpec.Builder.beanProvidersProperty() = apply {
        addProperty(
            PropertySpec
                .builder(
                    "providers", ClassName.bestGuess("ru.uporov.d.android.common.ProvidersMap")
                        .parameterizedBy(rootClassName, TypeVariableName("*"))
                )
                .initializer(mapOfProviders())
                .addModifiers(KModifier.OVERRIDE)
                .build()
        )
    }

    private fun mapOfProviders(): String {
        return """
        mutableMapOf(
        ${allDependencies.joinToString(",\n") {
            "${it.name}::class to ${it.name.asProviderParamName()}"
        }}
        )
        """.trimIndent()
    }

    private fun FunSpec.Builder.withProvidersLambdasParamsOf(dependencies: Set<Dependency>) = apply {
        dependencies.forEach {
            addParameter(
                ParameterSpec.builder(
                    it.name.asProviderParamName(),
                    LambdaTypeName.get(null, rootClassName, returnType = it.className)
                ).build()
            )
        }
    }

    private fun String.asProviderParamName() = PROVIDER_NAME_FORMAT.format(this).decapitalize()
}