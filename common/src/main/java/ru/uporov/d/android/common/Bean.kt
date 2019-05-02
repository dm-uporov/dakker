package ru.uporov.d.android.common

interface Bean {

    val trashCallback: TrashObservable
}

interface TrashObservable {

    fun subscribeOnTrashed(onTrashed: () -> Unit)
}