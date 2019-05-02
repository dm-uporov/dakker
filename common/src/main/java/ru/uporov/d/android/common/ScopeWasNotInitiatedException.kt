package ru.uporov.d.android.common

import kotlin.reflect.KClass

class ScopeWasNotInitiatedException(bean: KClass<*>): RuntimeException("${bean.java.simpleName}'s scope was not initiated")