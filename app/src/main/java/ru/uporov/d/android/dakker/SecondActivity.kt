package ru.uporov.d.android.dakker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.annotation.DakkerActivity
import ru.uporov.d.android.common.annotation.Inject

@DakkerActivity
class SecondActivity : AppCompatActivity() {

    @get:Inject
    val presenter: MainActivityViewModel by injectMainActivityViewModel()
    @get:Inject
    val thirdInteractor: ThirdInteractor by injectThirdInteractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startDakkerScope()
    }
}
