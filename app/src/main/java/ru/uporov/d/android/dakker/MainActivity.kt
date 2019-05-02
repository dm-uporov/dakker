package ru.uporov.d.android.dakker

import android.arch.lifecycle.GenericLifecycleObserver
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionBranch
import ru.uporov.d.android.common.Bean
import ru.uporov.d.android.dakker.DakkerApp.injectContext
import ru.uporov.d.android.dakker.DakkerApp.injectSomeInteractor

@InjectionBranch(
    dependencies = [Context::class, SomeInteractor::class]
)
class MainActivity : AppCompatActivity(), Bean {

    @get:Inject
    val context: Context by injectContext()
    @get:Inject
    val someInteractor: SomeInteractor by injectSomeInteractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DakkerApp.inject(this)
    }
}
