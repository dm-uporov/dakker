package ru.uporov.d.android.common

import kotlin.reflect.KClass

typealias ProvidersMap<O, T> = Map<KClass<T>, O.() -> T>