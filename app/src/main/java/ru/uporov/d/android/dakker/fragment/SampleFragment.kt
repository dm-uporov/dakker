package ru.uporov.d.android.dakker.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.dakker.activity.MainActivity
import ru.uporov.d.android.dakker.business.SampleFragmentPresenter

@DakkerScopeCore(parentScopeCoreClass = MainActivity::class)
class SampleFragment : Fragment() {

    @get:Inject
    val presenter: SampleFragmentPresenter by injectSampleFragmentPresenter()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter
        presenter
    }
}
