package ru.uporov.d.android.dakker.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.uporov.d.android.dakker.bindScopeToLifecycle

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindScopeToLifecycle()
    }
}