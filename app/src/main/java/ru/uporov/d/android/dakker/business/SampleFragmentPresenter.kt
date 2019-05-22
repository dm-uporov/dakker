package ru.uporov.d.android.dakker.business

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.LifecycleScope
import ru.uporov.d.android.dakker.fragment.SampleFragment

@LifecycleScope(coreClass = SampleFragment::class)
class SampleFragmentPresenter(thirdInteractor: ThirdInteractor<String>) : Fragment()
