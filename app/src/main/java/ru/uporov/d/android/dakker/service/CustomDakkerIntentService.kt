package ru.uporov.d.android.dakker.service

import android.content.Intent
import d.uporov.dakkerandroid.DakkerIntentService
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.dakker.ScopesIds.CUSTOM_DAKKER_INTENT_SERVICE_SCOPE_ID
import ru.uporov.d.android.dakker.business.ServiceHelper

@DakkerScopeCore(scopeId = CUSTOM_DAKKER_INTENT_SERVICE_SCOPE_ID)
class CustomDakkerIntentService : DakkerIntentService(CustomDakkerIntentService::class.java.simpleName) {

    @get:Inject
    private val helper: ServiceHelper by injectServiceHelper()

    override fun onBind(intent: Intent?) = null

    override fun onHandleIntent(intent: Intent?) {
        // handle intent
    }

    override fun onServiceDestroy() {
        super.onServiceDestroy()
        // last chance to call dependencies
    }
}