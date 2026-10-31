package com.example.receive.discovery

import com.example.receive.identity.DeviceIdentity
import com.example.receive.protocol.DeviceInfoDto
import com.example.receive.protocol.ProtocolJson

/** Pure helpers for LocalSend multicast messages. */
object DiscoveryMessages {
    const val GROUP = "224.0.0.167"
    const val PORT = 53317

    fun announcement(me: DeviceIdentity, announce: Boolean): ByteArray =
        ProtocolJson.encode(me.toDto(announce = announce)).toByteArray()

    fun parse(bytes: ByteArray): DeviceInfoDto? =
        runCatching { ProtocolJson.decode<DeviceInfoDto>(bytes.decodeToString()) }.getOrNull()

    /** Reply only to other devices that are announcing (avoids loops and self-discovery). */
    fun shouldRespond(msg: DeviceInfoDto, ownFingerprint: String) =
        msg.announce == true && msg.fingerprint != ownFingerprint
}
