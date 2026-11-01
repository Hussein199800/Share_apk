package com.example.receive

import com.example.receive.discovery.DiscoveryMessages
import com.example.receive.identity.DeviceIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryMessagesTest {
    private val me = DeviceIdentity("Pixel", "Pixel 8", "mine", 53317)

    @Test fun announcementRoundTrips() {
        val msg = DiscoveryMessages.parse(DiscoveryMessages.announcement(me, announce = true))!!
        assertEquals("Pixel", msg.alias)
        assertEquals(true, msg.announce)
        assertEquals(53317, msg.port)
    }

    @Test fun respondsOnlyToOthersAnnouncing() {
        val other = DiscoveryMessages.parse("""{"alias":"Desk","fingerprint":"other","announce":true,"port":53317}""".toByteArray())!!
        assertTrue(DiscoveryMessages.shouldRespond(other, me.fingerprint))
        assertFalse(DiscoveryMessages.shouldRespond(other.copy(announce = false), me.fingerprint))
        assertFalse(DiscoveryMessages.shouldRespond(other.copy(fingerprint = "mine"), me.fingerprint))
    }

    @Test fun garbageIsIgnored() = assertNull(DiscoveryMessages.parse("not json".toByteArray()))
}
