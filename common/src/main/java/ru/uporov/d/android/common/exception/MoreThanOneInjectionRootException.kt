package ru.uporov.d.android.common.exception

import ru.uporov.d.android.common.annotation.DakkerApplication

class MoreThanOneInjectionRootException(): RuntimeException("${DakkerApplication::class.simpleName} cannot be used on more than one class")