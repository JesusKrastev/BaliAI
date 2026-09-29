package com.jesuskrastev.bali

import android.content.Context
import android.provider.Settings

/**
 * Detects whether the app is being driven by Firebase Test Lab, so callers can keep that
 * traffic out of analytics and out of the push audience.
 *
 * Google Play's pre-launch report runs on Test Lab too, so every upload to a testing track
 * gets crawled by robots on a farm of real devices and emulators. Those robots open the app,
 * tap through the first screens and never finish onboarding; without this guard each of them
 * shows up as a brand-new user in PostHog, Mixpanel and Firebase and drags every funnel down.
 *
 * Unlike [RobolectricDetector], this does not stop SDKs from being created: the app still has
 * to run normally (RevenueCat, Firebase Auth and the paywall must work so the robots can
 * exercise them and the pre-launch report can surface crashes). Callers only silence the
 * SDKs that would record the robot as a user.
 */
object TestLabDetector {

    /** System setting Firebase Test Lab sets to `"true"` on every device it drives. */
    private const val TEST_LAB_SETTING = "firebase.test.lab"

    /**
     * Checks the Test Lab flag on the current device.
     *
     * @param context any context; only its content resolver is read
     * @return true when Firebase Test Lab (including Play's pre-launch report) is driving this
     *   device; false on a normal device, or if the setting can't be read
     */
    fun isTestLab(context: Context): Boolean = runCatching {
        Settings.System.getString(context.contentResolver, TEST_LAB_SETTING) == "true"
    }.getOrDefault(false)
}
