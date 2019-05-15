package ru.uporov.d.android.dakker

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore

@LifecycleScopeCore(parentScopeCoreClass = SecondActivity::class)
class SampleFragment : Fragment() {

    @get:Inject
    val presenter: SampleFragmentPresenter by injectSampleFragmentPresenter()
}
