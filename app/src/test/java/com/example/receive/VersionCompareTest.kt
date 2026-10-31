package com.example.receive

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionCompareTest {
    @Test fun notInstalledIsNew() = assertEquals(VersionStatus.NEW, compareWithInstalled(5, null))
    @Test fun higherIsUpdate() = assertEquals(VersionStatus.UPDATE, compareWithInstalled(6, 5))
    @Test fun equalIsSame() = assertEquals(VersionStatus.SAME, compareWithInstalled(5, 5))
    @Test fun lowerIsOlder() = assertEquals(VersionStatus.OLDER, compareWithInstalled(4, 5))
}
