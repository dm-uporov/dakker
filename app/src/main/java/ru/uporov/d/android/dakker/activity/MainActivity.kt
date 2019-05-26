package ru.uporov.d.android.dakker.activity

import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.business.ThirdInteractor


@DakkerScopeCore
class MainActivity : AppCompatActivity() {

    @get:Inject
    val viewModel: MainActivityViewModel by injectMainActivityViewModel()
    @get:Inject
    val thirdInteractorS: ThirdInteractor<String> by injectThirdInteractorString()
    @get:Inject
    val thirdInteractorI: ThirdInteractor<Int> by injectThirdInteractorInt()

}
