package com.jesuskrastev.bali.domain.model

/**
 * What came of asking, from inside the app, to let push notifications through again.
 *
 * @property analyticsValue the `result` sent with `notifications_permission_result`
 */
enum class EnablePushesResult(val analyticsValue: String) {
    /** Pushes can be shown now: the user granted the permission, or it was only opted out. */
    ENABLED("granted"),

    /** The system dialog was shown and the user said no. */
    DENIED("denied"),

    /**
     * Android will not show the dialog (the user said no twice, or Android 12 and below with the
     * app's notifications switched off): only the system settings can turn them back on.
     */
    NEEDS_SYSTEM_SETTINGS("system_settings")
}
