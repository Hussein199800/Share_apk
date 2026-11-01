package com.example.send

import android.content.Context

/** Persistent [PinStore]: remembered LocalSend device certificates survive app restarts. */
class PrefsPins(context: Context) : PinStore {
    private val prefs = context.getSharedPreferences("localsend_pins", Context.MODE_PRIVATE)
    override fun get(deviceFingerprint: String): String? = prefs.getString(deviceFingerprint, null)
    override fun put(deviceFingerprint: String, certSha256: String) {
        prefs.edit().putString(deviceFingerprint, certSha256).apply()
    }
}
