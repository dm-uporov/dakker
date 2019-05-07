package ru.uporov.d.android.common.exception

import ru.uporov.d.android.common.InjectionRoot

class NoInjectionRootException(): RuntimeException(
    "You must provide root with ${InjectionRoot::class.java.simpleName} annotation"
)