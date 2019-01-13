package ru.uporov.d.android.easydagger

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface DAppComponent {

    fun inject(mainActivity: MainActivity)
}