package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol
import ru.uporov.d.android.common.*
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
            InjectionRoot::class.java.name,
            InjectionBranch::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }

    private fun Symbol.ClassSymbol.getRequestedDependencies(): Set<String> {
        return enclosedElements
            .filter { it.getAnnotation(Inject::class.java) != null }
            .map { it.type.returnType.asTypeName().toString() }
            .toSet()
    }

    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        val targetsWithRequestedDependencies = mutableMapOf<ClassName, Set<String>>()
        fun generate(
            byAnnotation: KClass<out Annotation>,
            generation: (
                pack: String,
                className: String,
                root: DakkerRoot
            ) -> Unit
        ) {
            roundEnvironment.getElementsAnnotatedWith(byAnnotation.java)?.forEach { element ->
                if (element !is Symbol.ClassSymbol) throw IllegalAnnotationUsageException(InjectionRoot::class)

                val pack = processingEnv.elementUtils.getPackageOf(element).toString()
                val className = element.simpleName.toString()

                val target = element.asRoot()
                targetsWithRequestedDependencies[target.root.asClassName()] = element.getRequestedDependencies()
                generation(pack, className, target)
            }
        }
        generate(InjectionBranch::class, ::generateBranchModule)
        generate(InjectionRoot::class) { pack: String,
                                         className: String,
                                         root: DakkerRoot ->
            generateRootModule(pack, className, root, targetsWithRequestedDependencies)
        }
        return true
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
                            ClassName(pack, providersName(className)),
                            KModifier.PRIVATE,
                            KModifier.LATEINIT
                        )
                            .mutable(true)
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("init")
                            .addParameter("rootModule", ClassName(pack, providersName(className)))
                            .addStatement("root = rootModule")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("inject")
                            .addParameter("bean", Bean::class)
                            .addStatement("// TODO apply tree with corresponded ModuleProvider")
                            .build()
                    )
                    .addFunction(
                        FunSpec.builder("get")
                            .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                            .addTypeVariable(TypeVariableName("reified T"))
                            .receiver(Bean::class)
                            .returns(TypeVariableName("T"))
                            .addStatement("// TODO find dependency in the tree")
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
        val branchesParameters = mutableSetOf<String>()
        val providersParameters = mutableMapOf<ClassName, String>()
        addType(
            TypeSpec.classBuilder(providersName(className))
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .apply {
                            root.dependencies.forEach {
                                addParameter(
                                    ParameterSpec.builder(
                                        "${it.simpleName}Provider".decapitalize()
                                            .also { name -> providersParameters[it] = name },
                                        LambdaTypeName.get(
                                            null,
                                            ClassName(packageName = pack, simpleName = className),
                                            returnType = it
                                        )
                                    ).build()
                                )
                            }
                        }
                        .apply {
                            root.branches.forEach {
                                addParameter(
                                    "${it.simpleName}Branch".decapitalize()
                                        .also { name -> branchesParameters.add(name) },
                                    ClassName(pack, providersName(it.simpleName))
                                )
                            }
                        }
                        .build()
                )
                .addSuperinterface(DakkerProvider::class)
                .addProperty(
                    PropertySpec
                        .builder("beanClass", KClass::class.asClassName().parameterizedBy(root.root.asClassName()))
                        .initializer("${root.root.asClassName().simpleName}::class")
                        .build()
                )
                .addProperty(
                    PropertySpec
                        .builder("branches", Set::class.parameterizedBy(DakkerProvider::class))
                        .initializer("setOf(${branchesParameters.joinToString()})")
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
                        .initializer(
                            "mapOf(${providersParameters
                                .asIterable()
                                .joinToString { "${it.key.simpleName}::class to ${it.value}" }
                            })"
                        )
                        .build()
                )
                .build()
        )
    }

    private fun moduleName(className: String) = "Dakker$className"

    private fun providersName(className: String) = "DakkerProvider$className"

    private fun FileSpec.write() = writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))

    data class DakkerRoot(
        val root: Symbol.ClassSymbol,
        val branches: Set<ClassName> = setOf(),
        val dependencies: Set<ClassName> = setOf()
    )

    fun Symbol.ClassSymbol.asRoot(): DakkerRoot {
        val branches = mutableSetOf<ClassName>()
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
        return DakkerRoot(this, branches, dependencies)
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
