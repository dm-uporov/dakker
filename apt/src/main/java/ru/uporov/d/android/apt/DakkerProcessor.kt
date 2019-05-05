package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol
import ru.uporov.d.android.common.DakkerProvider
import ru.uporov.d.android.common.IllegalAnnotationUsageException
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionRoot
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(DakkerProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class DakkerProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(
            InjectionRoot::class.java.name
//            ,InjectionBranch::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        val targetsWithRequestedDependencies = mutableMapOf<ClassName, Set<String>>()
        //        generate(InjectionBranch::class, ::generateBranchModule)
        roundEnvironment.getElementsAnnotatedWith(InjectionRoot::class.java)?.forEach { element ->
            if (element !is Symbol.ClassSymbol) throw IllegalAnnotationUsageException(InjectionRoot::class)

            val pack = processingEnv.elementUtils.getPackageOf(element).toString()
            val className = element.simpleName.toString()

            val target = element.asRoot()
            targetsWithRequestedDependencies[target.root.asClassName()] =
                element.getRequestedDependencies().values.map(TypeName::toString).toSet()
            generateRootModule(pack, className, target, targetsWithRequestedDependencies)
        }
        return true
    }

    private fun Symbol.ClassSymbol.getRequestedDependencies(): Map<String, TypeName> {
        return enclosedElements
            .filter { it.getAnnotation(Inject::class.java) != null }
            .map { it.type.returnType.asTypeName() }
            .map { it.toString().substringAfterLast(".").decapitalize() to it }
            .toMap()
    }

    private fun generateRootModule(
        pack: String,
        className: String,
        root: DakkerRoot,
        requestedDependencies: Map<ClassName, Set<String>>
    ) {
        FileSpec.builder(pack, moduleName(className))
            // Provider of root
            .createProvider(pack, className, root)
            // Module
            .addType(
                TypeSpec.objectBuilder(moduleName(className))
                    .addProperty(
                        PropertySpec.builder(
                            "root",
                            ClassName(pack, providerName(className)),
                            KModifier.PRIVATE,
                            KModifier.LATEINIT
                        )
                            .mutable(true)
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("startDakker")
                            .receiver(root.root.asClassName())
                            .addParameter("rootModule", ClassName(pack, providerName(className)))
                            .addStatement("root = rootModule")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("inject")
                            .addParameter("bean", ClassName.bestGuess("androidx.lifecycle.LifecycleOwner"))
                            .addStatement("// TODO apply tree with corresponded ModuleProvider")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("get")
                            .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                            .addTypeVariable(TypeVariableName("reified T"))
                            .receiver(root.root.asClassName())
                            .returns(TypeVariableName("T"))
                            .addStatement(" return root.providers[T::class]?.invoke(this@get) as T")
                            .build()
                    )
                    .apply {
                        root.dependencies.forEach {
                            addFunction(
                                FunSpec.builder("get${it.simpleName}")
                                    .receiver(root.root.asClassName())
                                    .returns(it)
                                    .addStatement(" return get<${it.simpleName}>()")
                                    .build()
                            )
                        }
                    }
                    .apply {
                        requestedDependencies.forEach { className, set ->
                            set.forEach {
                                val simpleName = it.substringAfterLast(".")
                                addFunction(
                                    FunSpec.builder("inject$simpleName")
                                        .receiver(className)
                                        .returns(Lazy::class.asClassName().parameterizedBy(ClassName("", it)))
                                        .addStatement(" return lazy { get<$simpleName>() }")
                                        .build()
                                )
                            }
                        }
                    }
                    .build()
            )
            .apply {
                val dependencies = requestedDependencies.values.flatten()
                if (dependencies.isNotEmpty()) {
                    addImport("", *dependencies.toTypedArray())
                }
            }
            .build()
            .write()
    }

    private fun generateBranchModule(
        pack: String,
        className: String,
        root: DakkerRoot
    ) {
        FileSpec.builder(pack, moduleName(className))
            // Provider of root
            .createProvider(pack, className, root)
            .build()
            .write()
    }

    private fun FileSpec.Builder.createProvider(
        pack: String,
        className: String,
        root: DakkerRoot
    ): FileSpec.Builder = apply {
        val currentClassName = ClassName(packageName = pack, simpleName = className)
        val currentProviderName = providerName(className)
        val branchesParameters = mutableSetOf<String>()

        addType(
            TypeSpec.classBuilder(currentProviderName)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .withProvidersLambdas(root, currentClassName)
                        .apply {
                            root.branches.forEach {
                                addParameter(
                                    "${it.simpleName}Branch".decapitalize()
                                        .also { name -> branchesParameters.add(name) },
                                    ClassName(pack, providerName(it.simpleName))
                                )
                            }
                        }
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
                .superclass(DakkerProvider::class.asClassName().parameterizedBy(root.root.asClassName()))
                .addType(
                    TypeSpec.companionObjectBuilder()
                        .addFunction(
                            FunSpec.builder("${className.decapitalize()}Bean")
                                .returns(ClassName(pack, currentProviderName))
                                .withProvidersLambdas(root, currentClassName)
                                .addStatement(
                                    " return $currentProviderName(${root.requestedDependencies.keys.joinToString(",")
                                    { "${it}Provider" }})"
                                )
                                .build()
                        )
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder("beanClass", KClass::class.asClassName().parameterizedBy(root.root.asClassName()))
                        .initializer("${root.root.asClassName().simpleName}::class")
                        .addModifiers(KModifier.OVERRIDE)
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder(
                            "branches", Set::class.asClassName().parameterizedBy(
                                DakkerProvider::class.asClassName().parameterizedBy(TypeVariableName("*"))
                            )
                        )
                        .initializer("setOf(${branchesParameters.joinToString()})")
                        .addModifiers(KModifier.OVERRIDE)
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder(
                            "providers", Map::class.asTypeName().parameterizedBy(
                                KClass::class.asTypeName().parameterizedBy(TypeVariableName("*")),
                                LambdaTypeName.get(
                                    null,
                                    ClassName(packageName = pack, simpleName = className),
                                    returnType = Any::class.asTypeName()
                                )
                            )
                        )
                        .initializer(root.mapOfProviders())
                        .addModifiers(KModifier.OVERRIDE)
                        .build()
                )
                .build()
        )
    }

    // todo extract and keep as local value
    private fun FunSpec.Builder.withProvidersLambdas(root: DakkerRoot, currentClassName: ClassName) = apply {
        root.requestedDependencies.forEach {
            addParameter(
                ParameterSpec.builder(
                    "${it.key}Provider",
                    LambdaTypeName.get(null, currentClassName, returnType = it.value)
                ).build()
            )
        }
    }

    private fun DakkerRoot.mapOfProviders(): String {
        return "mapOf(${requestedDependencies.keys
            .joinToString { "${it.capitalize()}::class to ${it.decapitalize()}Provider" }
        })"
    }

    private fun moduleName(className: String) = "Dakker$className"

    private fun providerName(className: String) = "DakkerProvider$className"

    private fun FileSpec.write() = writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))

    data class DakkerRoot(
        val root: Symbol.ClassSymbol,
        val branches: Set<ClassName> = emptySet(),
        val dependencies: Set<ClassName> = emptySet(),
        val requestedDependencies: Map<String, TypeName> = emptyMap()
    )

    fun Symbol.ClassSymbol.asRoot(): DakkerRoot {
        val branches = mutableSetOf<ClassName>()
        val requestedDependencies = getRequestedDependencies()
        val dependencies = mutableSetOf<ClassName>()
        for (annotation in annotationMirrors) {
            for (pair in annotation.values) {
                when (pair.fst.simpleName.toString()) {
                    "branches" -> (pair.snd.value as? List<Attribute.Class>)
                        ?.toKclassList()
                        ?.let {
                            branches.addAll(it)
                        }
                    "dependencies" -> (pair.snd.value as? List<Attribute.Class>)
                        ?.toKclassList()
                        ?.let {
                            dependencies.addAll(it)
                        }
                }
            }
        }
        return DakkerRoot(this, branches, dependencies, requestedDependencies)
    }

    private fun List<Attribute.Class>.toKclassList() = map { it.classType.toString() }
        .map {
            val name = it.split('.').last()
            ClassName(it.substringBefore(".$name"), name)

        }

    override fun hashCode(): Int {
        return 1
    }
}
