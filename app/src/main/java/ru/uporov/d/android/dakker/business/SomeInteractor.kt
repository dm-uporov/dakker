package ru.uporov.d.android.dakker.business

import ru.uporov.d.android.common.annotation.ApplicationScope

class SomeInteractor @ApplicationScope constructor(anInteractor: AnInteractor, thirdInteractor: ThirdInteractor)

// TODO
// Стоит добавить возможность помечать как PerApplication просто функции, возвращающие, например, контекст
// или какой-нибудь внешний сервис, дабы не создавать постоянно классы
// !!! ТАКАЯ ВОЗМОЖНОСТЬ ДОЛЖНА БЫТЬ ТОЛЬКО ДЛЯ ФУНКЦИЙ ВНУТРИ ДРУГОГО PerApplication,
// дабы избежать непоняток со временем инициализации