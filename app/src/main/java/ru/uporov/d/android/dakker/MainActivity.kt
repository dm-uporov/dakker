package ru.uporov.d.android.dakker

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionNode
import ru.uporov.d.android.dakker.DakkerMainActivity.injectContext
import ru.uporov.d.android.dakker.DakkerMainActivity.injectSomeInteractor

//@InjectionBranch(
//    dependencies = [Context::class, SomeInteractor::class]
//)
@InjectionNode
class MainActivity : AppCompatActivity() {

    @get:Inject
    val context: Context by injectContext()
    @get:Inject
    val someInteractor: SomeInteractor by injectSomeInteractor()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        DakkerApp.inject(this)
//    }
}
