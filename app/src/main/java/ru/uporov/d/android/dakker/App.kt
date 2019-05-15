package ru.uporov.d.android.dakker

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProviders
import ru.uporov.d.android.common.annotation.DakkerApplication
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.provider.factory
import ru.uporov.d.android.common.provider.single
import ru.uporov.d.android.dakker.AppNode.Companion.appNode
import ru.uporov.d.android.dakker.Dakker.startDakker
import ru.uporov.d.android.dakker.activity.MainActivity
import ru.uporov.d.android.dakker.activity.MainActivityNode.Companion.mainActivityNode
import ru.uporov.d.android.dakker.activity.SecondActivityNode.Companion.secondActivityNode
import ru.uporov.d.android.dakker.business.AnInteractor
import ru.uporov.d.android.dakker.business.MainActivityViewModel
import ru.uporov.d.android.dakker.business.ThirdInteractor
import ru.uporov.d.android.dakker.fragment.DependentFragmentNode.Companion.dependentFragmentNode
import ru.uporov.d.android.dakker.fragment.SampleFragment
import ru.uporov.d.android.dakker.fragment.SampleFragmentNode.Companion.sampleFragmentNode

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
            appNode(single { this }, factory { object : ThirdInteractor {} }),
            mainActivityNode(
                single { ViewModelProviders.of(it).get(MainActivityViewModel::class.java) }
            ),
            secondActivityNode(
                single { ViewModelProviders.of(it).get(MainActivityViewModel::class.java) }
            ),
            sampleFragmentNode { activity as MainActivity },
            dependentFragmentNode { parentFragment as SampleFragment }
        )
    }
}