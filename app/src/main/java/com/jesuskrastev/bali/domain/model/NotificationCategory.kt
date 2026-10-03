package com.jesuskrastev.bali.domain.model

/**
 * A kind of push notification, with its own notification channel in Android.
 *
 * Every push the OneSignal journeys send names the channel of its category, so the user can
 * silence one kind from Android's settings and keep the rest: Android drops what arrives on a
 * channel that is switched off. Nothing is tagged or filtered in OneSignal for this.
 *
 * @property channelId id of the Android channel; never rename it, the messages in the OneSignal
 *   dashboard name it as "existing channel" and a renamed one would be created empty
 * @property title name shown in Settings and in Android's channel list
 * @property description what the category sends, shown under [title]
 */
enum class NotificationCategory(val channelId: String, val title: String, val description: String) {
    /** The id is the one the app has always created, so existing installs keep their choice. */
    STUDY(
        channelId = "reminders",
        title = "Recordatorios de estudio",
        description = "Tu hora de estudio y un aviso si llevas días sin abrir la app"
    ),
    STREAK(
        channelId = "streak",
        title = "Racha en peligro",
        description = "Un aviso cuando tu racha de días se va a perder"
    ),
    PROMOTIONS(
        channelId = "promotions",
        title = "Ofertas y novedades",
        description = "Descuentos y novedades de Bali AI"
    )
}
