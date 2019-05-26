package ru.uporov.d.android.common.exception

class NoScopeIdException(className: String): RuntimeException("You have to provide scopeId for $className")