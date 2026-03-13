package com.jesuskrastev.bali.ui.screens.onboarding

import com.onesignal.OneSignal

/**
 * NotificationManager handles all notification-related logic.
 * This abstraction keeps UI and business logic separated.
 */
interface INotificationManager {
    suspend fun requestNotificationPermission()
}

class NotificationManager : INotificationManager {
    override suspend fun requestNotificationPermission() {
        OneSignal.Notifications.requestPermission(false)
    }
}
