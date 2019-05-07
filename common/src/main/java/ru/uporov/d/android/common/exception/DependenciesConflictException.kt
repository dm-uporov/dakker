package ru.uporov.d.android.common.exception

class DependenciesConflictException(className: String) :
    RuntimeException("There is more than one providers for $className")