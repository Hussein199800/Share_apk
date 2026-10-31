package com.example.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class ReleaseInfo(val version: String, val notes: String, val apkUrl: String, val apkSize: Long)

enum class Verdict { Ok, WrongPackage, NotNewer, SignatureMismatch, Unreadable }

/** Pure parts of the GitHub-release updater (unit-tested). */
object UpdateLogic {
    @Serializable
    private data class Asset(val name: String = "", @SerialName("browser_download_url") val url: String = "", val size: Long = 0)

    @Serializable
    private data class Release(
        @SerialName("tag_name") val tag: String = "",
        val body: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<Asset> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun parseLatest(text: String): ReleaseInfo? {
        val r = runCatching { json.decodeFromString(Release.serializer(), text) }.getOrNull() ?: return null
        if (r.draft || r.prerelease) return null
        val apk = r.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: return null
        return ReleaseInfo(r.tag.trimStart('v', 'V'), r.body.orEmpty().trim(), apk.url, apk.size)
    }

    /** Numeric dotted-version comparison ("3.0.10" > "3.0.9"). */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trimStart('v', 'V').split('.', '-').map { it.toIntOrNull() ?: 0 }
        val a = parts(remote); val b = parts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** A downloaded APK may be installed only if it is this app, newer, and signed by the same key. */
    fun verify(
        candidatePackage: String?, candidateVersionCode: Long, candidateSigners: Set<String>,
        myPackage: String, myVersionCode: Long, mySigners: Set<String>
    ): Verdict = when {
        candidatePackage == null -> Verdict.Unreadable
        candidatePackage != myPackage -> Verdict.WrongPackage
        candidateVersionCode <= myVersionCode -> Verdict.NotNewer
        candidateSigners.isEmpty() || candidateSigners != mySigners -> Verdict.SignatureMismatch
        else -> Verdict.Ok
    }
}
