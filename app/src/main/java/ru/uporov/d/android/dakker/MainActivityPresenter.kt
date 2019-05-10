package ru.uporov.d.android.dakker

import android.util.Log
import ru.uporov.d.android.common.annotation.NodeScope

@NodeScope(MainActivity::class)
class MainActivityPresenter(someInteractor: SomeInteractor, mainInteactor: MainInteractor) {

    init {
        Log.d("TAAAG", "presenter is HERE!!!")
    }
}
