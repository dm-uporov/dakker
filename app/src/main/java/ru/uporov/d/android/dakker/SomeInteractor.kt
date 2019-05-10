package ru.uporov.d.android.dakker

import ru.uporov.d.android.common.annotation.ApplicationScope

class SomeInteractor @ApplicationScope constructor(anInteractor: AnInteractor)

// TODO
// Стоит добавить возможность помечать как PerApplication просто функции, возвращающие, например, контекст
// или какой-нибудь внешний сервис, дабы не создавать постоянно классы
// !!! ТАКАЯ ВОЗМОЖНОСТЬ ДОЛЖНА БЫТЬ ТОЛЬКО ДЛЯ ФУНКЦИЙ ВНУТРИ ДРУГОГО PerApplication,
// дабы избежать непоняток со временем инициализации