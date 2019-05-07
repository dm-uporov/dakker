package ru.uporov.d.android.common.exception

class DependencyIsNotProvidedException(className: String) :
    RuntimeException("$className cannot be injected because it is not provided")