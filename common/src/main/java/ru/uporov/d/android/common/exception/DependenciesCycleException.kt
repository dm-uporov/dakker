package ru.uporov.d.android.common.exception

import kotlin.reflect.KClass

class DependenciesCycleException(cls: KClass<*>) :
    RuntimeException("There is a dependencies cycle while providing ${cls.java.canonicalName}")