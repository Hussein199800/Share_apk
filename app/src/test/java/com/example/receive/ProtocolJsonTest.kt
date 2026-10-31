package com.example.receive

import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.PrepareUploadRequest
import com.example.receive.protocol.ProtocolJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtocolJsonTest {
    // Sample from the LocalSend v2 README, plus an unknown field.
    private val prepare = """
        {"info":{"alias":"Nice Orange","version":"2.0","deviceModel":"Samsung","deviceType":"mobile",
          "fingerprint":"random string","port":53317,"protocol":"https","download":true,"futureField":1},
         "files":{"a":{"id":"a","fileName":"my image.png","size":324242,"fileType":"image/jpeg",
           "sha256":"abc","preview":null,"metadata":{"modified":"2021-01-01T12:34:56Z"}},
          "b":{"id":"b","fileName":"Sakina.apk","size":1234,"fileType":"application/vnd.android.package-archive",
           "shareApk":{"packageName":"com.sakina","versionName":"1.3.0","versionCode":130}}}}
    """.trimIndent()

    @Test fun parsesReadmeSampleAndIgnoresUnknownFields() {
        val req = ProtocolJson.decode<PrepareUploadRequest>(prepare)
        assertEquals("Nice Orange", req.info.alias)
        assertEquals(324242L, req.files.getValue("a").size)
        assertNull(req.files.getValue("a").shareApk)
    }

    @Test fun readsShareApkExtension() {
        val meta = ProtocolJson.decode<PrepareUploadRequest>(prepare).files.getValue("b").shareApk!!
        assertEquals("com.sakina", meta.packageName)
        assertEquals(130L, meta.versionCode)
    }

    @Test fun deviceInfoRoundTrips() {
        val d = DeviceInfoDto(alias = "Pixel", version = "2.1", deviceModel = "Pixel 8", deviceType = "mobile",
            fingerprint = "f", port = 53317, protocol = "https", download = false)
        assertEquals(d, ProtocolJson.decode<DeviceInfoDto>(ProtocolJson.encode(d)))
    }
}
