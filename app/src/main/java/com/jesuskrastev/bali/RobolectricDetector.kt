package com.jesuskrastev.bali

/**
 * Detects whether the process is running under Robolectric, so callers can skip
 * initializing third-party SDKs (OneSignal, Mixpanel, PostHog, RevenueCat) that are known
 * to crash in that environment.
 *
 * Shared by [BaliApplication] and the Hilt modules that provide those SDK clients
 * ([com.jesuskrastev.bali.di.MixpanelModule], [com.jesuskrastev.bali.di.PostHogModule])
 * so there is exactly one place that decides this, instead of each call site guessing at
 * `Build.FINGERPRINT`'s exact value.
 */
object RobolectricDetector {

    /**
     * @return true when the current process is a Robolectric test.
     *
     * Checking for the `org.robolectric.Robolectric` class on the classpath is the
     * primary signal since it does not depend on any particular device fingerprint.
     * The `Build.FINGERPRINT`/`Build.DEVICE` substring check is a fallback for
     * environments where that class can't be loaded this way.
     */
    fun isRobolectric(): Boolean {
        return try {
            Class.forName("org.robolectric.Robolectric") != null
        } catch (e: ClassNotFoundException) {
            val fingerprint = android.os.Build.FINGERPRINT ?: ""
            fingerprint.contains("robolectric", ignoreCase = true) ||
                android.os.Build.DEVICE.contains("robolectric", ignoreCase = true)
        }
    }
}
