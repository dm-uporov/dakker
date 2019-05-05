package ru.uporov.d.android.common

import kotlin.reflect.KClass

typealias ProvidersMap<O, T> = MutableMap<KClass<T>, O.() -> T>