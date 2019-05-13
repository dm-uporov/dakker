package ru.uporov.d.android.dakker

import android.util.Log
import androidx.lifecycle.ViewModel

var count = 0

class MainActivityViewModel: ViewModel() {

    init {
        Log.d("TAAAG", "count is ${++count}")
    }
}