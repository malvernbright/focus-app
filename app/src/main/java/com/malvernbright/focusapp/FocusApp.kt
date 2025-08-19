package com.malvernbright.focusapp

import android.app.Application
import com.malvernbright.focusapp.notifications.NotificationHelper

class FocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}

