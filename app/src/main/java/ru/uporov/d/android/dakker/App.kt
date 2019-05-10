package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.InjectionRoot
import ru.uporov.d.android.dakker.AppNode.Companion.appNode
import ru.uporov.d.android.dakker.Dakker.startDakker
import ru.uporov.d.android.dakker.DakkerApp.injectAnInteractor
import ru.uporov.d.android.dakker.DakkerApp.injectContext
import ru.uporov.d.android.dakker.MainActivityNode.Companion.mainActivityNode

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
            appNode { this },
            mainActivityNode()
        )
    }
}