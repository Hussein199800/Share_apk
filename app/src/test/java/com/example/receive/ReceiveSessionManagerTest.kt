package com.example.receive

import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.FileDto
import com.example.receive.protocol.PrepareUploadRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class ReceiveSessionManagerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val sender = DeviceInfoDto(alias = "Desk", fingerprint = "x")
    private fun file(id: String, name: String, bytes: ByteArray = ByteArray(10) { it.toByte() }, sha: String? = null) =
        FileDto(id = id, fileName = name, size = bytes.size.toLong(), sha256 = sha)
    private fun req(vararg f: FileDto) = PrepareUploadRequest(sender, f.associateBy { it.id })
    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private fun manager(timeout: Long = 60_000) = ReceiveSessionManager(tmp.newFolder(), timeout) { "S" + (++n) }
    private var n = 0

    @Test fun messageOnlyRequestNeedsNoTransfer() = runTest {
        val m = manager()
        val msg = FileDto(id = "t", fileName = "msg.txt", size = 5, fileType = "text/plain", preview = "hello")
        val r = async { m.prepare(req(msg), "10.0.0.2") }
        runCurrent(); m.decide(true)
        assertEquals(PrepareResult.NoTransfer, r.await())
        assertEquals(listOf("hello"), (m.state.value as ReceiveState.Done).messages)
    }

    @Test fun allFileTypesAreAccepted() = runTest {
        val m = manager()
        val r = async { m.prepare(req(file("a", "photo.png"), file("b", "Sakina.apk"), file("c", "x.XAPK")), "10.0.0.2") }
        runCurrent()
        val pending = (m.state.value as ReceiveState.AwaitingDecision).request
        assertEquals(3, pending.files.size)
        m.decide(true)
        val ok = r.await() as PrepareResult.Accepted
        assertEquals(setOf("a", "b", "c"), ok.response.files.keys)
    }

    @Test fun rejectAndTimeoutBothReject() = runTest {
        val m = manager(timeout = 1_000)
        val r1 = async { m.prepare(req(file("b", "a.apk")), "ip") }
        runCurrent(); m.decide(false)
        assertEquals(PrepareResult.Rejected, r1.await())
        val r2 = async { m.prepare(req(file("b", "a.apk")), "ip") }
        advanceTimeBy(1_500)
        assertEquals(PrepareResult.Rejected, r2.await())
        assertEquals(ReceiveState.Idle, m.state.value)
    }

    @Test fun secondSenderIsBlockedDuringSession() = runTest {
        val m = manager()
        val r = async { m.prepare(req(file("b", "a.apk")), "ip") }
        runCurrent()
        assertEquals(PrepareResult.Blocked, m.prepare(req(file("c", "c.apk")), "ip2"))
        m.decide(true); r.await()
        assertEquals(PrepareResult.Blocked, m.prepare(req(file("c", "c.apk")), "ip2"))
    }

    private suspend fun kotlinx.coroutines.test.TestScope.accepted(m: ReceiveSessionManager, vararg f: FileDto, ip: String = "ip"): PrepareResult.Accepted {
        val r = async { m.prepare(req(*f), ip) }
        runCurrent(); m.decide(true)
        return r.await() as PrepareResult.Accepted
    }

    @Test fun uploadWritesFileAndFinishes() = runTest {
        val m = manager()
        val bytes = ByteArray(300_000) { (it % 251).toByte() }
        val ok = accepted(m, file("b", "Sakina.apk", bytes, sha(bytes)))
        val res = m.receive(ok.response.sessionId, "b", ok.response.files.getValue("b"), "ip", bytes.inputStream())
        assertEquals(UploadResult.Ok, res)
        val done = m.state.value as ReceiveState.Done
        assertEquals(bytes.size.toLong(), done.files.single().file.length())
    }

    @Test fun wrongTokenOrIpIsForbidden() = runTest {
        val m = manager()
        val ok = accepted(m, file("b", "a.apk"))
        val sid = ok.response.sessionId
        assertEquals(UploadResult.Forbidden, m.receive(sid, "b", "nope", "ip", ByteArray(10).inputStream()))
        assertEquals(UploadResult.Forbidden, m.receive(sid, "b", ok.response.files.getValue("b"), "other", ByteArray(10).inputStream()))
        assertEquals(UploadResult.Forbidden, m.receive("bad", "b", ok.response.files.getValue("b"), "ip", ByteArray(10).inputStream()))
    }

    @Test fun checksumMismatchDeletesFile() = runTest {
        val m = manager()
        val ok = accepted(m, file("b", "a.apk", sha = "00"))
        val res = m.receive(ok.response.sessionId, "b", ok.response.files.getValue("b"), "ip", ByteArray(10).inputStream())
        assertEquals(UploadResult.ChecksumMismatch, res)
        assertFalse(m.sessionDir(ok.response.sessionId).listFiles().orEmpty().any { it.length() > 0 })
    }

    @Test fun pathTraversalAndDuplicateNamesAreSanitized() = runTest {
        val m = manager()
        val ok = accepted(m, file("a", "../../evil.apk"), file("b", "evil.apk"))
        val sid = ok.response.sessionId
        m.receive(sid, "a", ok.response.files.getValue("a"), "ip", ByteArray(10).inputStream())
        m.receive(sid, "b", ok.response.files.getValue("b"), "ip", ByteArray(10).inputStream())
        val names = (m.state.value as ReceiveState.Done).files.map { it.file }
        assertTrue(names.all { it.parentFile == m.sessionDir(sid) })
        assertEquals(2, names.map { it.name }.toSet().size)
        // Folder structure from the sender is kept (sanitized) for saving into Downloads.
        assertEquals(listOf("", ""), (m.state.value as ReceiveState.Done).files.map { it.relativeDir })
    }

    @Test fun cancelDeletesPartialFilesAndResets() = runTest {
        val m = manager()
        val ok = accepted(m, file("b", "a.apk"), file("c", "c.apk"))
        val sid = ok.response.sessionId
        m.receive(sid, "b", ok.response.files.getValue("b"), "ip", ByteArray(10).inputStream())
        m.cancel(sid)
        assertFalse(m.sessionDir(sid).exists())
        assertEquals(ReceiveState.Idle, m.state.value)
    }

    @Test fun autoRefusalsAreReportedAsEvents() = runTest {
        val m = manager(timeout = 1_000)
        val r = async { m.prepare(req(file("b", "a.apk")), "ip") }
        advanceTimeBy(1_500); r.await()
        assertEquals(ReceiveEvent.TimedOut("Desk"), m.lastEvent.value)
    }

    @Test fun cancelledPrepareDoesNotWedgeState() = runTest {
        val m = manager()
        val r = async { m.prepare(req(file("b", "a.apk")), "ip") }
        runCurrent()
        r.cancel() // e.g. server stopped or sender disconnected
        runCurrent()
        assertEquals(ReceiveState.Idle, m.state.value)
        val r2 = async { m.prepare(req(file("c", "c.apk")), "ip") }
        runCurrent()
        assertTrue(m.state.value is ReceiveState.AwaitingDecision)
        m.decide(false); r2.await()
    }

    @Test fun failedUploadCancelsSession() = runTest {
        val m = manager()
        val ok = accepted(m, file("b", "a.apk"))
        val broken = object : java.io.InputStream() { override fun read(): Int = throw java.io.IOException("peer gone") }
        val res = m.receive(ok.response.sessionId, "b", ok.response.files.getValue("b"), "ip", broken)
        assertTrue(res is UploadResult.Error)
        assertEquals(ReceiveState.Idle, m.state.value)
    }

    @Test fun idleSessionIsCancelledByWatchdog() = runTest {
        val m = manager()
        val ok = accepted(m, file("b", "a.apk"))
        m.expireIfIdle(nowMs = Long.MAX_VALUE)
        assertEquals(ReceiveState.Idle, m.state.value)
        assertFalse(m.sessionDir(ok.response.sessionId).exists())
    }

    @Test fun uploadLargerThanDeclaredIsRejected() = runTest {
        val m = manager()
        val ok = accepted(m, file("b", "a.apk", ByteArray(10)))
        val res = m.receive(ok.response.sessionId, "b", ok.response.files.getValue("b"), "ip", ByteArray(5000).inputStream())
        assertTrue(res is UploadResult.Error)
    }

    @Test fun folderPathsAreKeptSanitized() {
        assertEquals("Trip/Day 1", ReceiveSessionManager.relativeDirOf("Trip/Day 1/a.jpg"))
        assertEquals("x", ReceiveSessionManager.relativeDirOf("../../x/a.jpg"))
        assertEquals("", ReceiveSessionManager.relativeDirOf("a.jpg"))
    }
}
