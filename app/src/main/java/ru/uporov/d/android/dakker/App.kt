package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import ru.uporov.d.android.common.Bean
import ru.uporov.d.android.common.Inject
import ru.uporov.d.android.common.InjectionRoot
import ru.uporov.d.android.dakker.DakkerApp.getContext
import ru.uporov.d.android.dakker.DakkerApp.injectContext
import ru.uporov.d.android.dakker.DakkerApp.startDakker
import ru.uporov.d.android.dakker.DakkerProviderApp.Companion.appBean
import ru.uporov.d.android.dakker.DakkerProviderMainActivity.Companion.mainActivityBean
import ru.uporov.d.android.dakker.DakkerProviderMainActivity.Companion.parentBean
import ru.uporov.d.android.dakker.DakkerProviderSecondActivity.Companion.parentBean
import ru.uporov.d.android.dakker.DakkerProviderSecondActivity.Companion.secondActivityBean

@InjectionRoot(
    branches = [MainActivity::class, SecondActivity::class],
    dependencies = [Context::class]
)
class App : Application(), Bean {

    @get:Inject
    private val context: Context by injectContext()

    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDakker()
    }

    private fun initDakker() {
        startDakker(appBean(
            { this },
            mainActivityBean(
                { parentBean().getContext() },
                { SomeInteractor(parentBean().getContext()) }
            ),
            secondActivityBean(
                { parentBean().getContext() },
                { AnotherInteractor(parentBean().getContext()) }
            )
        ))
    }
}