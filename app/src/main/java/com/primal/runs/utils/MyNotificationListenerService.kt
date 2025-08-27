package com.primal.runs.utils

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (packageName == "com.spotify.music") {
            Log.d("SpotifySession", "Spotify is playing!")
        }
    }
}