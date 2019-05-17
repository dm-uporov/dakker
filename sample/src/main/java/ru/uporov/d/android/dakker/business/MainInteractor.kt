package ru.uporov.d.android.dakker.business

import ru.uporov.d.android.common.annotation.LifecycleScope
import ru.uporov.d.android.dakker.activity.MainActivity

@LifecycleScope(MainActivity::class, isSinglePerScope = false)
class MainInteractor
