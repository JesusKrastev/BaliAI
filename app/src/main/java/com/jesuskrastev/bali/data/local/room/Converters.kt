package com.jesuskrastev.bali.data.local.room

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromLongList(value: List<Long>?): String {
        if (value == null) return "[]"
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
