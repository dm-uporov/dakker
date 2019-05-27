package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProviders
import ru.uporov.d.android.common.annotation.DakkerApplication
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.provider.factory
import ru.uporov.d.android.common.provider.single
import ru.uporov.d.android.dakker.AppModule.Companion.appModule
import ru.uporov.d.android.dakker.Dakker.startDakker
import ru.uporov.d.android.dakker.activity.MainActivity
import ru.uporov.d.android.dakker.activity.MainActivityModule.Companion.mainActivityModule
import ru.uporov.d.android.dakker.activity.SecondActivityModule.Companion.secondActivityModule
import ru.uporov.d.android.dakker.business.*
import ru.uporov.d.android.dakker.fragment.DependentFragmentModule.Companion.dependentFragmentModule
import ru.uporov.d.android.dakker.fragment.SampleFragment
import ru.uporov.d.android.dakker.fragment.SampleFragmentModule.Companion.sampleFragmentModule
import ru.uporov.d.android.dakker.fragment.getThirdInteractorString

@DakkerApplication
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
            appModule(
                single { this },
                factory { object : ThirdInteractor<Context> {} }
            ),
            mainActivityModule(
                single { ViewModelProviders.of(it).get(MainActivityViewModel::class.java) },
                factory { object : ThirdInteractor<String> {} },
                factory { object : ThirdInteractor<Int> {} },
                single { MainInteractor() }
            ),
            secondActivityModule(
                single { ViewModelProviders.of(it).get(MainActivityViewModel::class.java) },
                factory { object : ThirdInteractor<String> {} }
            ),
            sampleFragmentModule(
                { activity as MainActivity },
                single { SampleFragmentPresenter(it.getThirdInteractorString()) }
            ),
            dependentFragmentModule { parentFragment as SampleFragment }
        )
    }
}