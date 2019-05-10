package ru.uporov.d.android.dakker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.DakkerActivity
import ru.uporov.d.android.common.annotation.Inject

@DakkerActivity
class MainActivity : AppCompatActivity() {

    @get:Inject
    val presenter: MainActivityPresenter by injectMainActivityPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startDakkerScope()
    }
}
