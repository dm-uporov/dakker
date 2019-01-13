package ru.uporov.d.android.common

class EasyDaggerContext private constructor(val component: EasyDaggerComponent) {

    companion object {
        lateinit var instance: EasyDaggerContext

        fun init(component: EasyDaggerComponent) {
            instance = EasyDaggerContext(component)
        }
    }

}