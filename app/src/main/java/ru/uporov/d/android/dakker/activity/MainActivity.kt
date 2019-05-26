package ru.uporov.d.android.dakker.activity

import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.dakker.ScopesIds
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.business.ThirdInteractor


@DakkerScopeCore(ScopesIds.MAIN_ACTIVITY_SCOPE_ID)
class MainActivity : AppCompatActivity() {

    @get:Inject
    val viewModel: MainActivityViewModel by injectMainActivityViewModel()
    @get:Inject
    val thirdInteractorS: ThirdInteractor<String> by injectThirdInteractorString()
    @get:Inject
    val thirdInteractorI: ThirdInteractor<Int> by injectThirdInteractorInt()

}
