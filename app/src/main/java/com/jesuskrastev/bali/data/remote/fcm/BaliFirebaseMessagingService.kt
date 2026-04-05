package com.jesuskrastev.bali.data.remote.fcm

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jesuskrastev.bali.BuildConfig

/**
 * Listens for FCM token refreshes and saves the new token to the user's
 * Firestore document so Cloud Functions can deliver notifications.
 *
 * The initial token fetch on login is handled by MainViewModel.
 * This service only needs to react to token refreshes (rare).
 */
class BaliFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("env")
            .document(BuildConfig.BUILD_TYPE)
            .collection("users")
            .document(userId)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // FCM displays the notification automatically when the app is in background.
        // Add foreground notification handling here if needed.
    }
}
