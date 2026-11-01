package com.example.receive

import com.example.installer.PackageInstallerManager.Companion.pickBaseApkEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseApkPickTest {
    @Test fun apksUsesBaseApk() = assertEquals("base.apk", pickBaseApkEntry(listOf("split_config.arm64_v8a.apk", "base.apk")))
    @Test fun xapkUsesPackageNamedApk() = assertEquals("com.app.apk", pickBaseApkEntry(listOf("config.arm64_v8a.apk", "com.app.apk", "config.ar.apk")))
    @Test fun fallsBackToFirst() = assertEquals("split_a.apk", pickBaseApkEntry(listOf("split_a.apk")))
}
