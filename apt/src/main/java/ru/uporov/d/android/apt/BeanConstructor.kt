package ru.uporov.d.android.apt

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import ru.uporov.d.android.common.DakkerBean
import kotlin.reflect.KClass

private const val MODULE_NAME_FORMAT = "Dakker%s"
private const val BEAN_NAME_FORMAT = "DakkerBean%s"
private const val PROVIDER_NAME_FORMAT = "%sProvider"

class BeanConstructor(
    private val pack: String,
    private val rootName: String,
    private val scopeDependencies: List<DakkerProcessor.PerScopeElement>,
    private val requestedDependencies: Map<String, TypeName>
) {

    private val moduleName = MODULE_NAME_FORMAT.format(rootName)
    private val beanName = BEAN_NAME_FORMAT.format(rootName)

    private val rootClassName = ClassName.bestGuess("$pack.$rootName")
    private val beanClassName = ClassName("$pack.$moduleName", beanName)


    fun build(): FileSpec {
        return generateModule()
    }

    private fun generateModule(): FileSpec {
        return FileSpec.builder(pack, moduleName)
            // Module
            .addType(
                TypeSpec.objectBuilder(moduleName)
                    .addProperty(
                        PropertySpec.builder(
                            "root",
                            beanClassName,
                            KModifier.PRIVATE,
                            KModifier.LATEINIT
                        )
                            .mutable(true)
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("startDakker")
                            .receiver(rootClassName)
                            .addParameter("rootModule", beanClassName)
                            .addStatement("root = rootModule")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("get")
                            .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                            .addTypeVariable(TypeVariableName("reified T"))
                            .receiver(rootClassName)
                            .returns(TypeVariableName("T"))
                            .addStatement(" return root.providers[T::class]?.invoke(this@get) as T")
                            .build()
                    )
                    .apply {
                        requestedDependencies.values.map(TypeName::toString).forEach {
                            val simpleName = it.substringAfterLast(".")
                            addFunction(
                                FunSpec.builder("inject$simpleName")
                                    .receiver(rootClassName)
                                    .returns(Lazy::class.asClassName().parameterizedBy(ClassName("", it)))
                                    .addStatement(" return lazy { get<$simpleName>() }")
                                    .build()
                            )
                        }
                    }
                    .apply {
                        scopeDependencies.forEach {
                            addFunction(
                                FunSpec.builder("get${it.className.capitalize()}")
                                    .receiver(rootClassName)
                                    .returns(ClassName.bestGuess(it.qualifiedName))
                                    .addStatement(" return get<${it.className}>()")
                                    .build()
                            )
                        }
                    }
                    .withBean()
                    .build()
            )
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

    private fun TypeSpec.Builder.withBean(): TypeSpec.Builder = apply {
        addType(
            TypeSpec.classBuilder(beanName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .withProvidersLambdas()
                        .apply {
                            scopeDependencies.forEach {
                                addParameter(
                                    ParameterSpec.builder(
                                        PROVIDER_NAME_FORMAT.format(it.className.decapitalize()),
                                        LambdaTypeName.get(
                                            null,
                                            rootClassName,
                                            returnType = ClassName.bestGuess(it.qualifiedName)
                                        )
                                    ).build()
                                )
                            }
                        }
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
                .superclass(DakkerBean::class.asClassName().parameterizedBy(rootClassName))
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(
                            FunSpec.builder("${rootName.decapitalize()}Bean")
                                .returns(ClassName("$pack.$moduleName", beanName))
                                .withProvidersLambdas()
                                .addStatement(
                                    """ return $beanName(${
                                    requestedDependencies.keys.joinToString { PROVIDER_NAME_FORMAT.format(it) }
                                    },
                                    ${scopeDependencies.joinToString { element ->
                                        "{ ${element.qualifiedName}(" +
                                                element.params.joinToString { "it.get<${it.type}>()" } +
                                                ")}"
                                    }}
                                    )
                                    """.trimIndent()
                                )
                                .build()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder("beanClass", KClass::class.asClassName().parameterizedBy(rootClassName))
                        .initializer("$rootName::class")
                        .addModifiers(KModifier.OVERRIDE)
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder(
                            "providers", ClassName.bestGuess("ru.uporov.d.android.common.ProvidersMap").parameterizedBy(
                                rootClassName,
                                TypeVariableName("*")
                            )
                        )
                        .initializer(mapOfProviders())
                        .addModifiers(KModifier.OVERRIDE)
                        .build()
                )
                .build()
        )
    }

    private fun FunSpec.Builder.withProvidersLambdas() = apply {
        requestedDependencies.forEach {
            addParameter(
                ParameterSpec.builder(
                    PROVIDER_NAME_FORMAT.format(it.key),
                    LambdaTypeName.get(null, rootClassName, returnType = it.value)
                ).build()
            )
        }
    }

    private fun mapOfProviders(): String {
        return """
            mutableMapOf(${requestedDependencies.keys
            .joinToString { "${it.capitalize()}::class to ${PROVIDER_NAME_FORMAT.format(it)}" }},
            ${scopeDependencies.joinToString { "${it.className.capitalize()}::class to ${PROVIDER_NAME_FORMAT.format(it.className.decapitalize())}" }}
            )
        """.trimIndent()
    }
}