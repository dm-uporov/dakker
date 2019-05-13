package ru.uporov.d.android.dakker

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.DakkerFragment
import ru.uporov.d.android.common.annotation.Inject

@DakkerFragment
class SampleFragment : Fragment() {

    @get:Inject
    val presenter: SampleFragmentPresenter by injectSampleFragmentPresenter()
}
