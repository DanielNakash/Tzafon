package com.thefoxworks.tzafon.data.sync

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlin.math.floor

/**
 * Converts a flat Room entity to/from a structured Firestore document map.
 *
 * The Room rows are deliberately flat (primitives + nullable strings; lists
 * already CSV-encoded), so each entity maps 1:1 to a Firestore document with
 * real fields. Gson does the field walking; the number bridge keeps whole
 * numbers as Long (not Double) so timestamps stay clean in Firestore.
 * Unknown keys on read (e.g. a future field) are ignored by Gson.
 */
object FirestoreCodec {
    private val gson = Gson()

    fun toMap(entity: Any): Map<String, Any?> {
        val obj = gson.toJsonTree(entity).asJsonObject
        val out = LinkedHashMap<String, Any?>(obj.size())
        for ((k, v) in obj.entrySet()) out[k] = jsonToValue(v)
        return out
    }

    fun <T> fromMap(map: Map<String, Any?>, clazz: Class<T>): T {
        val obj = JsonObject()
        for ((k, v) in map) obj.add(k, gson.toJsonTree(v))
        return gson.fromJson(obj, clazz)
    }

    private fun jsonToValue(v: JsonElement): Any? = when {
        v.isJsonNull -> null
        v.isJsonPrimitive -> {
            val p = v.asJsonPrimitive
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> {
                    val d = p.asDouble
                    if (!d.isInfinite() && d == floor(d)) d.toLong() else d
                }
                else -> p.asString
            }
        }
        else -> v.toString()
    }
}
