package ru.uporov.d.android.easydagger

import android.app.Application
import android.content.Context
import ru.uporov.d.android.common.EasyDagger
import ru.uporov.d.android.common.EasyDaggerComponent.Companion.component
import ru.uporov.d.android.common.EasyModule

@EasyDagger
class App : Application() {

    companion object {
        lateinit var instance: App
    }

    @EasyModule(providersFor = [Context::class, SomeInteractor::class])
    fun app() = component {
        module {
            provider<Context> { this@App }
            provider { SomeInteractor(get()) }
        }
    }

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        instance = this
        initDagger()
    }

    private fun initDagger() {
        appComponent = DaggerAppComponent.builder()
            .appModule(AppModule(this))
            .build()
    }
}