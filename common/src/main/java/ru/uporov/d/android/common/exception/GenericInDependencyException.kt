package ru.uporov.d.android.common.exception

class GenericInDependencyException(type: String) :
    RuntimeException("$type. Cannot to provide dependency with generic type.")