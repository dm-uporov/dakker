package ru.uporov.d.android.dakker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.InjectionNode

@InjectionNode
class MainActivity : AppCompatActivity() {

    @get:Inject
    val presenter: MainActivityPresenter by injectMainActivityPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("TAAAG", "activity was created")
    }
}
