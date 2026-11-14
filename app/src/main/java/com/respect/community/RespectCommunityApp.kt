package com.respect.community

import android.app.Application
import com.respect.community.di.AppContainer
import com.respect.community.service.KickAnalyticsWorker
import com.respect.community.service.NotificationHelper

class RespectCommunityApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)
        KickAnalyticsWorker.schedule(this)
    }

    companion object {
        const val CHANNEL_LIVE_ALERTS = NotificationHelper.CHANNEL_LIVE_ALERTS
        const val CHANNEL_MONITOR_SERVICE = NotificationHelper.CHANNEL_MONITOR_SERVICE
    }
}
