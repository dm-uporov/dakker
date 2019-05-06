package ru.uporov.d.android.dakker

import android.content.Context
import ru.uporov.d.android.common.PerApplication

class AnInteractor @PerApplication constructor(private val context: Context)

// т.е. по факту мы в конструктор запрашиваем зависимости. При генерации кода смотрим,
// предоставлены ли запрашиваемые зависимости как также PerApplication.
// Если да, матчим, строим граф. Если нет - ошибка компиляции.

// Стоит добавить возможность помечать как PerApplication просто функции, возвращающие, например, контекст
// или какой-нибудь внешний сервис, дабы не создавать постоянно классы
// !!! ТАКАЯ ВОЗМОЖНОСТЬ ДОЛЖНА БЫТЬ ТОЛЬКО ДЛЯ ФУНКЦИЙ ВНУТРИ ДРУГОГО PerApplication,
// дабы избежать непоняток со временем инициализации