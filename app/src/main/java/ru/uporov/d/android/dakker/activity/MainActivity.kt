package ru.uporov.d.android.dakker.activity

import android.os.Bundle
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.R
import ru.uporov.d.android.dakker.injectMainActivityViewModel

@LifecycleScopeCore
class MainActivity : BaseActivity() {

    @get:Inject
    val presenter: MainActivityViewModel by injectMainActivityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
