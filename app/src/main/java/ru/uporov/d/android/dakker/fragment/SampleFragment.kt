package ru.uporov.d.android.dakker.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.dakker.ScopesIds
import ru.uporov.d.android.dakker.business.SampleFragmentPresenter

@DakkerScopeCore(
    scopeId = ScopesIds.SAMPLE_FRAGMENT_SCOPE_ID,
    parentScopeId = ScopesIds.MAIN_ACTIVITY_SCOPE_ID
)
class SampleFragment : Fragment() {

    @get:Inject
    val presenter: SampleFragmentPresenter by injectSampleFragmentPresenter()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter
        presenter
    }
}
