package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProviders
import ru.uporov.d.android.common.annotation.DakkerApplication
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.provider.single
import ru.uporov.d.android.dakker.AppNode.Companion.appNode
import ru.uporov.d.android.dakker.Dakker.startDakker
import ru.uporov.d.android.dakker.MainActivityNode.Companion.mainActivityNode
import ru.uporov.d.android.dakker.SampleFragmentNode.Companion.sampleFragmentNode
import ru.uporov.d.android.dakker.SecondActivityNode.Companion.secondActivityNode

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
            appNode(single { this }),
            mainActivityNode(single { ViewModelProviders.of(it).get(MainActivityViewModel::class.java) }),
            sampleFragmentNode { this.activity as SecondActivity },
            secondActivityNode(single { ViewModelProviders.of(it).get(MainActivityViewModel::class.java) })
        )
    }
}