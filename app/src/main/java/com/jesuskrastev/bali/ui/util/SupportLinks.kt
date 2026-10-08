package com.jesuskrastev.bali.ui.util

import android.net.Uri

/** Direct contact channels with the Bali team, opened from Settings. */
object SupportLinks {

    /** WhatsApp number of the team in international format without "+" (Spain, 642 04 05 66). */
    private const val WHATSAPP_NUMBER = "34642040566"

    /** Message pre-filled in the chat so the user only has to hit send. */
    private const val WHATSAPP_GREETING = "¡Hola Bali! Os escribo para dar mi opinión sobre la app: "

    /**
     * Builds the click-to-chat link of the team's WhatsApp.
     *
     * @return a wa.me URL that opens WhatsApp (or the browser if it is not installed)
     */
    fun whatsappUrl(): String = "https://wa.me/$WHATSAPP_NUMBER?text=${Uri.encode(WHATSAPP_GREETING)}"
}
