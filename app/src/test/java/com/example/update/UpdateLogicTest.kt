package com.example.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateLogicTest {
    private val latestJson = """
        {"tag_name":"v3.1.0","name":"شارك أبك 3.1.0","body":"- إرسال أسرع","draft":false,"prerelease":false,
         "assets":[{"name":"notes.txt","browser_download_url":"https://x/notes.txt","size":10},
                   {"name":"shareapk-3.1.0.apk","browser_download_url":"https://x/shareapk-3.1.0.apk","size":12345}]}
    """.trimIndent()

    @Test fun parsesLatestReleaseAndPicksApkAsset() {
        val r = UpdateLogic.parseLatest(latestJson)!!
        assertEquals("3.1.0", r.version)
        assertEquals("https://x/shareapk-3.1.0.apk", r.apkUrl)
        assertEquals(12345L, r.apkSize)
        assertEquals("- إرسال أسرع", r.notes)
    }

    @Test fun releaseWithoutApkIsIgnored() =
        assertNull(UpdateLogic.parseLatest("""{"tag_name":"v3.1.0","assets":[]}"""))

    @Test fun semverComparison() {
        assertTrue(UpdateLogic.isNewer("3.1.0", "3.0.0"))
        assertTrue(UpdateLogic.isNewer("3.0.10", "3.0.9"))
        assertTrue(UpdateLogic.isNewer("4", "3.9.9"))
        assertFalse(UpdateLogic.isNewer("3.0.0", "3.0.0"))
        assertFalse(UpdateLogic.isNewer("2.9", "3.0.0"))
    }

    @Test fun verifyCandidate() {
        val sig = setOf("aa")
        assertEquals(Verdict.Ok, UpdateLogic.verify("com.shareapk.app", 31000, sig, "com.shareapk.app", 30000, sig))
        assertEquals(Verdict.WrongPackage, UpdateLogic.verify("com.other", 31000, sig, "com.shareapk.app", 30000, sig))
        assertEquals(Verdict.NotNewer, UpdateLogic.verify("com.shareapk.app", 30000, sig, "com.shareapk.app", 30000, sig))
        assertEquals(Verdict.SignatureMismatch, UpdateLogic.verify("com.shareapk.app", 31000, setOf("bb"), "com.shareapk.app", 30000, sig))
    }
}
