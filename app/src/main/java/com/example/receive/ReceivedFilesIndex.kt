package com.example.receive

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.example.receive.protocol.ProtocolJson
import kotlinx.serialization.Serializable
import java.io.File

/** A received non-app file (saved in Downloads/ShareAPK) or text message. */
@Serializable
data class ReceivedEntry(
    val name: String,
    val uri: String? = null,
    val mimeType: String? = null,
    val size: Long = 0,
    val text: String? = null,
    val receivedAt: Long = System.currentTimeMillis()
) {
    val isText get() = text != null
}

/** Small persisted list (newest first, capped) of received files and messages. */
class ReceivedFilesIndex(context: Context) {
    private val file = File(context.filesDir, "received_index.json")

    @Synchronized
    fun load(): List<ReceivedEntry> =
        runCatching { ProtocolJson.decode<List<ReceivedEntry>>(file.readText()) }.getOrDefault(emptyList())

    @Synchronized
    fun add(entries: List<ReceivedEntry>): List<ReceivedEntry> {
        val all = (entries + load()).take(MAX)
        runCatching { file.writeText(ProtocolJson.encode(all)) }
        return all
    }

    @Synchronized
    fun remove(entry: ReceivedEntry): List<ReceivedEntry> {
        val all = load().filterNot { it == entry }
        runCatching { file.writeText(ProtocolJson.encode(all)) }
        return all
    }

    companion object { const val MAX = 200 }
}

/** Copies a received file into Download/ShareAPK[/relativeDir] via MediaStore (no storage permission). */
object DownloadsSaver {
    fun mimeFor(name: String, declared: String?): String =
        declared?.takeIf { it.contains('/') && it != "application/octet-stream" }
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

    fun save(context: Context, src: File, displayName: String, mime: String, relativeDir: String): Uri? {
        val resolver = context.contentResolver
        val dir = listOf(Environment.DIRECTORY_DOWNLOADS, "ShareAPK", relativeDir).filter { it.isNotEmpty() }.joinToString("/")
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)!!.use { out -> src.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        }.getOrElse {
            resolver.delete(uri, null, null)
            null
        }
    }
}
