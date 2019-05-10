package ru.uporov.d.android.common.provider

fun <O, T> factory(provide: (O) -> T) = FactoryProvider(provide)

class FactoryProvider<O, T> internal constructor(
    private val provider: (O) -> T
) : Provider<O, T> {

    override fun invoke(scopeOwner: O): T = provider(scopeOwner)
}