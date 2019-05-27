package ru.uporov.d.android.dakker.business

import android.content.Context
import ru.uporov.d.android.common.annotation.ApplicationScope

class AnInteractor @ApplicationScope(isSinglePerScope = false) constructor(context: Context, thirdInteractor: ThirdInteractor<Context>)