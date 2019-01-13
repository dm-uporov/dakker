package ru.uporov.d.android.common.exception

import kotlin.reflect.KClass

class CannotFindDependencyException(type: KClass<*>): RuntimeException("Cannot find dependency of type ${type.simpleName}")