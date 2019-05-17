package ru.uporov.d.android.common.exception

import kotlin.reflect.KClass

class IllegalAnnotationUsageException(annotation: KClass<*>) :
    RuntimeException("Annotation ${annotation.java.canonicalName} can be used only with classes")