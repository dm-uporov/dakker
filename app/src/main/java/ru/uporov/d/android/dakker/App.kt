package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionRoot
import ru.uporov.d.android.dakker.DakkerApp.AppBean.Companion.appBean
import ru.uporov.d.android.dakker.DakkerApp.injectAnInteractor
import ru.uporov.d.android.dakker.DakkerApp.injectContext
import ru.uporov.d.android.dakker.DakkerApp.startDakker

@InjectionRoot
class App : Application() {

    @get:Inject
    private val context: Context by injectContext()
    @get:Inject
    private val interactor: AnInteractor by injectAnInteractor()

    override fun onCreate() {
        super.onCreate()
        initDakker()
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