package com.example.installer

import com.example.data.model.BackupAppEntry
import com.example.data.model.BatchRestoreSummary
import com.example.data.model.RestoreAppStatus
import com.example.data.model.RestoreCandidateApp
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandsTest {
    @Test fun parsesExitCodeOutputAndReasons() {
        val ok = ShellCommands.parse("0\nSuccess\n")
        assertTrue(ok.ok)
        val bad = ShellCommands.parse("1\nFailure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match]\n")
        assertFalse(bad.ok)
        assertEquals("INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match", bad.reason)
        assertFalse(ShellCommands.parse("0\nFailure [DELETE_FAILED_INTERNAL_ERROR]").ok)
        assertEquals(-1, ShellCommands.parse("garbage").code)
    }

    @Test fun readsSessionIdAndBuildsCommands() {
        assertEquals(1234, ShellCommands.sessionId("Success: created install session [1234]"))
        assertNull(ShellCommands.sessionId("Failure"))
        assertArrayEquals(arrayOf("pm", "install-write", "-S", "10", "7", "base.apk", "-"), ShellCommands.installWrite(7, 10, "base.apk"))
        assertArrayEquals(arrayOf("pm", "uninstall", "-k", "--user", "0", "com.x"), ShellCommands.uninstallForUser("com.x"))
    }

    @Test fun criticalPackagesAreProtected() {
        listOf("com.android.systemui", "com.android.providers.media.module", "com.google.android.gms", "android").forEach {
            assertTrue(it, ShellCommands.isProtected(it))
        }
        assertFalse(ShellCommands.isProtected("com.facebook.appmanager"))
    }
}

class BatchSummaryTest {
    private fun app(p: String) = RestoreCandidateApp(
        BackupAppEntry(p, p, "1", 1, 31, 35, false, emptyList(), null, null, 1, emptyList()),
        RestoreAppStatus.NOT_INSTALLED, null, null, true, true
    )

    @Test fun retriedAppsLeaveTheReportAndResultsMerge() {
        val s = BatchRestoreSummary(listOf(app("a")), listOf(app("b") to "x"), listOf(app("c"), app("d")))
        val carry = s.without(setOf("b", "c"))
        assertEquals(listOf("d"), carry.skipped.map { it.entry.packageName })
        assertTrue(carry.failed.isEmpty())
        val merged = carry + BatchRestoreSummary(installed = listOf(app("b"), app("c")))
        assertEquals(3, merged.installedCount)
        assertEquals(1, merged.skippedCount)
    }
}
