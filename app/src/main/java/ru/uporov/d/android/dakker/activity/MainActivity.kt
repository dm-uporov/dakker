package ru.uporov.d.android.dakker.activity

import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.business.ThirdInteractor


@LifecycleScopeCore
class MainActivity : AppCompatActivity() {

    @get:Inject
    val viewModel: MainActivityViewModel by injectMainActivityViewModel()
    @get:Inject
    val thirdInteractor: ThirdInteractor by injectThirdInteractor()

}
