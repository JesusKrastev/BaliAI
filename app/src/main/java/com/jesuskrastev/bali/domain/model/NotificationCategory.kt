package com.jesuskrastev.bali.domain.model

/**
 * A kind of push notification the user can switch off on its own from Settings.
 *
 * The categories an opted-out user has are sent to OneSignal as a single `notif_off` tag (the
 * free plan keeps 6 tags per user, so one tag per category does not fit), and the journeys in
 * the OneSignal dashboard skip whoever is opted out of theirs.
 *
 * @property key stable value inside the `notif_off` tag; never rename it, the OneSignal
 *   journeys filter on it
 * @property title name shown in Settings
 * @property description what the category sends, shown under [title]
 */
enum class NotificationCategory(val key: String, val title: String, val description: String) {
    STUDY(
        key = "study",
        title = "Recordatorios de estudio",
        description = "Tu hora de estudio, la racha en peligro y un aviso si llevas días sin abrir la app"
    ),
    PROMOTIONS(
        key = "promos",
        title = "Ofertas y novedades",
        description = "Descuentos y novedades de Bali AI"
    );

    companion object {
        /**
         * Builds the value of the `notif_off` tag for [disabled]: the keys in enum order, joined
         * by commas, so the same choice always gives the same text (`study`, `promos` or
         * `study,promos`).
         *
         * @param disabled the categories the user switched off
         * @return the tag value, or null when nothing is switched off and the tag must go
         */
        fun offTagValue(disabled: Set<NotificationCategory>): String? =
            entries.filter { it in disabled }.joinToString(",") { it.key }.ifEmpty { null }

        /**
         * Reads categories back from their [key]s, ignoring any this version does not know.
         *
         * @param keys values previously stored from [NotificationCategory.key]
         * @return the matching categories
         */
        fun fromKeys(keys: Set<String>): Set<NotificationCategory> =
            entries.filterTo(mutableSetOf()) { it.key in keys }
    }
}
