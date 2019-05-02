package ru.uporov.d.android.common

import kotlin.reflect.KClass

class IllegalAnnotationUsageException(annotation: KClass<*>): RuntimeException("${annotation.simpleName} can be used only with classes")