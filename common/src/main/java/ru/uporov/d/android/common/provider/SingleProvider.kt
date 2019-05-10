package ru.uporov.d.android.common.provider

// Provider for scope single dependency
fun <O, T> single(provide: (O) -> T) = SingleProvider(provide)

class SingleProvider<O, T> internal constructor(
    private val provider: (O) -> T
) : Provider<O, T> {

    private var value: T? = null

    override fun invoke(scopeOwner: O): T {
        with(value) {
            if (this == null) {
                val newValue = provider(scopeOwner)
                value = newValue
                return newValue
            }
            return this
        }
    }

    override fun trashValue() {
        value = null
    }
}