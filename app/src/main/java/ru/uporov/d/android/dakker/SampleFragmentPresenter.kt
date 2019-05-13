package ru.uporov.d.android.dakker

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.FragmentScope

@FragmentScope(coreClass = SampleFragment::class)
class SampleFragmentPresenter(thirdInteractor: ThirdInteractor) : Fragment()
