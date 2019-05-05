package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import android.support.multidex.MultiDex
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionRoot
import ru.uporov.d.android.common.Stub
import ru.uporov.d.android.dakker.DakkerApp.DakkerBeanApp.Companion.appBean
import ru.uporov.d.android.dakker.DakkerApp.injectAnInteractor
import ru.uporov.d.android.dakker.DakkerApp.injectContext
import ru.uporov.d.android.dakker.DakkerApp.startDakker

@InjectionRoot
class App : Application() {

    @get:Inject
    private val context: Context by injectContext()
    @get:Inject
    private val interactor: AnInteractor by injectAnInteractor()

    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        instance = this
        initDakker()
        context.toString()
    }

    private fun initDakker() {
        startDakker(
            appBean(
                contextProvider = { it }
            )
        )
//        startDakker(appBean(
//            { this }
////            mainActivityBean(
////                { parentBean().getContext() },
////                { SomeInteractor(parentBean().getContext()) }
////            ),
////            secondActivityBean(
////                { parentBean().getContext() },
////                { AnotherInteractor(parentBean().getContext()) }
////            )
//        ))
    }
}