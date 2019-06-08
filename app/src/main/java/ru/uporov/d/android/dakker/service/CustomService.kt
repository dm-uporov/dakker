package ru.uporov.d.android.dakker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import ru.uporov.d.android.common.Destroyable
import ru.uporov.d.android.common.OnDestroyObserver
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.dakker.ScopesIds.CUSTOM_SERVICE_SCOPE_ID
import ru.uporov.d.android.dakker.business.ThirdInteractor

@DakkerScopeCore(scopeId = CUSTOM_SERVICE_SCOPE_ID)
class CustomService : Service(), Destroyable {

    private val onDestroyObservers = mutableSetOf<OnDestroyObserver>()

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        // one of parent dependencies
        val thirdInteractor: ThirdInteractor<Context> = getThirdInteractorContext()
    }

    override fun addObserver(onDestroyObserver: OnDestroyObserver) {
        onDestroyObservers.add(onDestroyObserver)
    }

    override fun removeObserver(onDestroyObserver: OnDestroyObserver) {
        onDestroyObservers.remove(onDestroyObserver)
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyObservers.forEach(OnDestroyObserver::onDestroy)
    }
}