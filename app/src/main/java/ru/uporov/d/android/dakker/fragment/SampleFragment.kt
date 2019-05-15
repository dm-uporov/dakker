package ru.uporov.d.android.dakker.fragment

import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.common.annotation.LifecycleScopeCore
import ru.uporov.d.android.dakker.activity.SecondActivity
import ru.uporov.d.android.dakker.business.SampleFragmentPresenter

@LifecycleScopeCore(parentScopeCoreClass = SecondActivity::class)
class SampleFragment : BaseFragment() {

    @get:Inject
    val presenter: SampleFragmentPresenter by injectSampleFragmentPresenter()
}
