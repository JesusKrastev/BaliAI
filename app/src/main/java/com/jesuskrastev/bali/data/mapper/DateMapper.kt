package com.jesuskrastev.bali.data.mapper

import java.util.Calendar
import java.util.Date

fun Date.toTimestamp(): Long {
    val calendar = Calendar.getInstance()
    calendar.time = this

    return calendar.timeInMillis
}

fun Long.toDate(): Date = Date(this)