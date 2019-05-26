package ru.uporov.d.android.dakker.fragment

import androidx.fragment.app.Fragment
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.dakker.ScopesIds

@DakkerScopeCore(
    scopeId = ScopesIds.DEPENDENT_FRAGMENT_SCOPE_ID,
    parentScopeId = ScopesIds.SAMPLE_FRAGMENT_SCOPE_ID
)
class DependentFragment : Fragment()
