package ru.uporov.d.android.dakker

import ru.uporov.d.android.common.ApplicationScope

@ApplicationScope
class ThirdInteractor {

    private val abc = ""
    val def = 16
    var ghi = 20.0

    fun maa() {

    }
}

// TODO
// Стоит добавить возможность помечать как PerApplication просто функции, возвращающие, например, контекст
// или какой-нибудь внешний сервис, дабы не создавать постоянно классы
// !!! ТАКАЯ ВОЗМОЖНОСТЬ ДОЛЖНА БЫТЬ ТОЛЬКО ДЛЯ ФУНКЦИЙ ВНУТРИ ДРУГОГО PerApplication,
// дабы избежать непоняток со временем инициализации