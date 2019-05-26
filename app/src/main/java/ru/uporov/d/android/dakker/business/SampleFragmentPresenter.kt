package ru.uporov.d.android.dakker.business

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.DakkerScope
import ru.uporov.d.android.dakker.fragment.SampleFragment

@DakkerScope(coreClass = SampleFragment::class)
class SampleFragmentPresenter(thirdInteractor: ThirdInteractor<String>) : Fragment()
