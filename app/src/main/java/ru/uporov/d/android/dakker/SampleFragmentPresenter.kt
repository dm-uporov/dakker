package ru.uporov.d.android.dakker

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.LifecycleScope

@LifecycleScope(coreClass = SampleFragment::class)
class SampleFragmentPresenter(thirdInteractor: ThirdInteractor) : Fragment()
