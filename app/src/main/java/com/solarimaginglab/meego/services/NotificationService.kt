package com.solarimaginglab.meego.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf

data class MeeGoNote(val title: String, val text: String, val packageName: String)

class NotificationService : NotificationListenerService() {
    companion object {
        val currentNotifications = mutableStateListOf<MeeGoNote>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = sbn?.notification?.extras ?: return
        val title = extras.getString("android.title") ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        currentNotifications.removeAll { it.packageName == sbn.packageName && it.title == title }
        currentNotifications.add(0, MeeGoNote(title, text, sbn.packageName))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        currentNotifications.removeAll { it.packageName == sbn?.packageName }
    }
}