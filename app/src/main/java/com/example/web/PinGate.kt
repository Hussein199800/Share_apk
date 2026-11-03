package com.example.web

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Access control for the browser page: a 4-digit PIN shown on the phone buys a random session token.
 * After [maxFails] wrong guesses the PIN is replaced, so guessing all 10 000 codes is impractical.
 */
class PinGate(
    private val random: SecureRandom = SecureRandom(),
    private val maxFails: Int = 5,
    private val onPinChanged: (String) -> Unit = {}
) {
    @Volatile var pin: String = newPin()
        private set

    private val tokens = ConcurrentHashMap.newKeySet<String>()
    private var fails = 0

    /** Returns a session token for the right PIN, or null. */
    @Synchronized
    fun login(candidate: String): String? {
        val ok = MessageDigest.isEqual(candidate.trim().toByteArray(), pin.toByteArray())
        if (ok) {
            fails = 0
            return newToken().also(tokens::add)
        }
        if (++fails >= maxFails) {
            fails = 0
            pin = newPin()
            onPinChanged(pin)
        }
        return null
    }

    fun isValid(token: String?): Boolean = token != null && token in tokens

    private fun newPin() = "%04d".format(random.nextInt(10_000))

    private fun newToken(): String {
        val b = ByteArray(16).also(random::nextBytes)
        return b.joinToString("") { "%02x".format(it) }
    }
}
