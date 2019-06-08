package ru.uporov.d.android.common.exception

class SeveralScopesUseTheSameIdException(id: Int) : RuntimeException("Several scopes use id $id")