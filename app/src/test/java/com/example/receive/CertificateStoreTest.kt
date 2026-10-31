package com.example.receive

import com.example.receive.identity.CertificateStore
import com.example.receive.identity.DeviceIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CertificateStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun fingerprintIsStableAcrossLoads() {
        val dir = tmp.newFolder()
        val a = CertificateStore(dir).load()
        val b = CertificateStore(dir).load()
        assertEquals(a.fingerprint, b.fingerprint)
        assertTrue(a.fingerprint.matches(Regex("[0-9a-f]{64}")))
    }

    @Test fun identityBuildsLocalSendInfo() {
        val info = DeviceIdentity(alias = "Pixel 8", deviceModel = "Pixel 8", fingerprint = "ab", port = 53317).toDto(announce = true)
        assertEquals("https", info.protocol)
        assertEquals("mobile", info.deviceType)
        assertEquals(true, info.announce)
    }
}
