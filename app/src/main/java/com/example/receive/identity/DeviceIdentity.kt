package com.example.receive.identity

import com.example.receive.protocol.DeviceInfoDto

/** How this device presents itself to LocalSend peers. */
data class DeviceIdentity(
    val alias: String,
    val deviceModel: String?,
    val fingerprint: String,
    val port: Int
) {
    fun toDto(announce: Boolean? = null) = DeviceInfoDto(
        alias = alias,
        version = PROTOCOL_VERSION,
        deviceModel = deviceModel,
        deviceType = "mobile",
        fingerprint = fingerprint,
        port = port,
        protocol = "https",
        download = false,
        announce = announce,
        shareApk = true
    )

    companion object {
        const val PROTOCOL_VERSION = "2.1"
        const val DEFAULT_PORT = 53317
    }
}
