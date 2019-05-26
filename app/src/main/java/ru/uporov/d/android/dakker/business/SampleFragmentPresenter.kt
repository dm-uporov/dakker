package ru.uporov.d.android.dakker.business

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.DakkerScope
import ru.uporov.d.android.dakker.ScopesIds

@DakkerScope(ScopesIds.SAMPLE_FRAGMENT_SCOPE_ID)
class SampleFragmentPresenter(thirdInteractor: ThirdInteractor<String>) : Fragment()
