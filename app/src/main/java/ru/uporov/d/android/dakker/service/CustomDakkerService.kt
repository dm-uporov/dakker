package ru.uporov.d.android.dakker.service

import android.content.Intent
import d.uporov.dakkerandroid.DakkerService
import ru.uporov.d.android.common.annotation.DakkerScopeCore
import ru.uporov.d.android.common.annotation.Inject
import ru.uporov.d.android.dakker.ScopesIds.CUSTOM_DAKKER_SERVICE_SCOPE_ID
import ru.uporov.d.android.dakker.business.ServiceHelper

@DakkerScopeCore(scopeId = CUSTOM_DAKKER_SERVICE_SCOPE_ID)
class CustomDakkerService : DakkerService() {

    @get:Inject
    private val helper: ServiceHelper by injectServiceHelper()

    override fun onBind(intent: Intent?) = null

    override fun onServiceDestroy() {
        super.onServiceDestroy()

    }
}