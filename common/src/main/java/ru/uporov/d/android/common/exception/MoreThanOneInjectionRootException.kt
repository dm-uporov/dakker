package ru.uporov.d.android.common.exception

import ru.uporov.d.android.common.annotation.DakkerApplication

class MoreThanOneInjectionRootException
    : RuntimeException("Annotation ${DakkerApplication::class.java.canonicalName} cannot be used for more than one class")