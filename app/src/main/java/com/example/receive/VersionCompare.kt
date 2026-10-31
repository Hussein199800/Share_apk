package com.example.receive

/** How an incoming package relates to what is installed. */
enum class VersionStatus { NEW, UPDATE, SAME, OLDER }

fun compareWithInstalled(incomingVersionCode: Long, installedVersionCode: Long?): VersionStatus = when {
    installedVersionCode == null -> VersionStatus.NEW
    incomingVersionCode > installedVersionCode -> VersionStatus.UPDATE
    incomingVersionCode == installedVersionCode -> VersionStatus.SAME
    else -> VersionStatus.OLDER
}
