package com.jesuskrastev.bali.data.mapper

import com.jesuskrastev.bali.data.remote.firestore.entities.SuggestionFirestore
import com.jesuskrastev.bali.domain.model.Suggestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Suggestion.toSuggestionFirestore(): SuggestionFirestore =
    SuggestionFirestore(
        text = text,
        userId = userId,
        email = email,
        timestamp = timestamp,
        date = timestamp.toDateString()
    )

private fun Long.toDateString(): String {
    val date = Date(this)
    val format = SimpleDateFormat("d MMMM yyyy", Locale("es", "ES"))
    return format.format(date)
}