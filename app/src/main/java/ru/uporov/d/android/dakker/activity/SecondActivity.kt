package ru.uporov.d.android.dakker.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore
import ru.uporov.d.android.dakker.R
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.business.ThirdInteractor

@LifecycleScopeCore
class SecondActivity : AppCompatActivity() {

    @get:Inject
    val presenter: MainActivityViewModel by injectMainActivityViewModel()
    @get:Inject
    val thirdInteractor: ThirdInteractor<String> by injectThirdInteractorString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
