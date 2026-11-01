package com.example.receive.protocol

import kotlinx.serialization.json.Json

/** Lenient JSON for LocalSend messages: unknown fields ignored, nulls tolerated. */
object ProtocolJson {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    inline fun <reified T> decode(text: String): T = json.decodeFromString(kotlinx.serialization.serializer<T>(), text)
    inline fun <reified T> encode(value: T): String = json.encodeToString(kotlinx.serialization.serializer<T>(), value)
}
