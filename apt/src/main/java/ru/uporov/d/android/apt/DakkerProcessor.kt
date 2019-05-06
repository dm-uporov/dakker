package ru.uporov.d.android.apt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.sun.tools.javac.code.Symbol
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionRoot
import ru.uporov.d.android.common.PerApplication
import ru.uporov.d.android.common.exception.IllegalAnnotationUsageException
import ru.uporov.d.android.common.exception.MoreThanOneInjectionRootException
import java.io.File
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

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
            PerApplication::class.java.name
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latest()
    }


    override fun process(set: MutableSet<out TypeElement>?, roundEnvironment: RoundEnvironment): Boolean {
        val perApplicationElements =
            roundEnvironment.getElementsAnnotatedWith(PerApplication::class.java)?.map { element ->
                if (element !is Symbol.MethodSymbol) throw IllegalAnnotationUsageException(
                    PerApplication::class
                )
                Dependency(
                    processingEnv.elementUtils.getPackageOf(element).toString(),
                    element.enclClass().simpleName.toString(),
                    element.params().map {
                        Dependency(it.packge().toString(), it.type.toString())
                    }
                )
            }

        val injectionRoots = roundEnvironment.getElementsAnnotatedWith(InjectionRoot::class.java)
        if (injectionRoots?.count() ?: 0 > 1) {
            throw MoreThanOneInjectionRootException()
        }
        injectionRoots?.forEach { element ->
            if (element !is Symbol.ClassSymbol) throw IllegalAnnotationUsageException(
                InjectionRoot::class
            )
            BeanBuilder(
                pack = processingEnv.elementUtils.getPackageOf(element).toString(),
                rootName = element.simpleName.toString(),
                scopeDependencies = perApplicationElements ?: emptyList(),
                requestedDependencies = element.getRequestedDependencies()
            )
                .build()
                .write()

        }
        return true
    }

    private fun Symbol.ClassSymbol.getRequestedDependencies(): List<Dependency> {
        return enclosedElements
            .filter { it.getAnnotation(Inject::class.java) != null }
            .map { it.type.returnType.asTypeName() }
            .map(TypeName::toString)
            .map {
                Dependency(
                    it.substringBeforeLast("."),
                    it.substringAfterLast(".")
                )
            }
    }

    private fun FileSpec.write() = writeTo(File(processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]))

    override fun hashCode(): Int {
        return 1
    }
}
