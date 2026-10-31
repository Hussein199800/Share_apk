package com.example.receive.protocol

import kotlinx.serialization.Serializable

/** LocalSend v2 device info (register / announce / prepare-upload `info`). */
@Serializable
data class DeviceInfoDto(
    val alias: String,
    val version: String = "2.1",
    val deviceModel: String? = null,
    val deviceType: String? = null,
    val fingerprint: String,
    val port: Int? = null,
    val protocol: String? = null,
    val download: Boolean = false,
    val announce: Boolean? = null,
    /** ShareAPK extension so peers can label us; LocalSend ignores it. */
    val shareApk: Boolean? = null
)

/** ShareAPK-only extension; other LocalSend clients ignore it. */
@Serializable
data class ShareApkMetaDto(
    val packageName: String,
    val versionName: String? = null,
    val versionCode: Long
)

@Serializable
data class FileDto(
    val id: String,
    val fileName: String,
    val size: Long,
    val fileType: String? = null,
    val sha256: String? = null,
    val preview: String? = null,
    val shareApk: ShareApkMetaDto? = null
)

@Serializable
data class PrepareUploadRequest(val info: DeviceInfoDto, val files: Map<String, FileDto>)

@Serializable
data class PrepareUploadResponse(val sessionId: String, val files: Map<String, String>)
