package com.jesuskrastev.bali.data.mapper

import java.util.Date

/** Converts to epoch milliseconds. */
fun Date.toTimestamp(): Long = time

/** Converts epoch milliseconds to a [Date]. */
fun Long.toDate(): Date = Date(this)