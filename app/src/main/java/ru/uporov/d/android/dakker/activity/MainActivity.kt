package ru.uporov.d.android.dakker.activity

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.business.ThirdInteractor
import ru.uporov.d.android.dakker.fragment.SampleFragment

@LifecycleScopeCore
class MainActivity : AppCompatActivity() {

    @get:Inject
    val viewModel: MainActivityViewModel by injectMainActivityViewModel()
    @get:Inject
    val thirdInteractor: ThirdInteractor by injectThirdInteractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction()
            .add(android.R.id.content, SampleFragment())
            .commit()

        Handler().postDelayed({
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SampleFragment())
                .commit()
        }, 10000)
    }
}
