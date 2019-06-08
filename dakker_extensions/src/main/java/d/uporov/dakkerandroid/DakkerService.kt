package d.uporov.dakkerandroid

import android.app.Service
import ru.uporov.d.android.common.Destroyable
import ru.uporov.d.android.common.OnDestroyObserver

abstract class DakkerService: Service(), Destroyable {

    private val onDestroyObservers = mutableSetOf<OnDestroyObserver>()

    final override fun addObserver(onDestroyObserver: OnDestroyObserver) {
        onDestroyObservers.add(onDestroyObserver)
    }

    final override fun removeObserver(onDestroyObserver: OnDestroyObserver) {
        onDestroyObservers.remove(onDestroyObserver)
    }

    final override fun onDestroy() {
        super.onDestroy()
        onServiceDestroy()
        onDestroyObservers.forEach(OnDestroyObserver::onDestroy)
    }

    protected open fun onServiceDestroy() { }
}