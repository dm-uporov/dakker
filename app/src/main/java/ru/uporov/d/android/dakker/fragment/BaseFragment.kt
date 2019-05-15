package ru.uporov.d.android.dakker.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import ru.uporov.d.android.dakker.bindScopeToLifecycle

abstract class BaseFragment : Fragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindScopeToLifecycle()
    }
}
