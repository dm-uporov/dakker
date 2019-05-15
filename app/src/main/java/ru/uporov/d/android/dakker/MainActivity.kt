package ru.uporov.d.android.dakker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore

@LifecycleScopeCore
class MainActivity : AppCompatActivity() {

    @get:Inject
    val presenter: MainActivityViewModel by injectMainActivityViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startDakkerScope()
        presenter
    }
}
