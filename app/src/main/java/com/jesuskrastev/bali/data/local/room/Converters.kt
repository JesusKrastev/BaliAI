package com.jesuskrastev.bali.data.local.room

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    /** Returns reward claim ids in [value] as a JSON array for Room. */
    @TypeConverter
    fun fromStringList(value: List<String>?): String = Json.encodeToString(value.orEmpty())

    /** Returns reward claim ids from [value], or an empty list for malformed legacy data. */
    @TypeConverter
    fun toStringList(value: String?): List<String> = try {
        if (value.isNullOrEmpty()) emptyList() else Json.decodeFromString(value)
    } catch (error: Exception) {
        emptyList()
    }

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

    @TypeConverter
    fun fromNodeStatus(value: com.jesuskrastev.bali.domain.model.NodeStatus): String {
        return value.name
    }

    @TypeConverter
    fun toNodeStatus(value: String): com.jesuskrastev.bali.domain.model.NodeStatus {
        return try {
            com.jesuskrastev.bali.domain.model.NodeStatus.valueOf(value)
        } catch (e: Exception) {
            com.jesuskrastev.bali.domain.model.NodeStatus.LOCKED
        }
    }
}
