package com.example.ui.components.design

import com.example.data.preferences.sanitizeTextScale
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveTest {
    @Test fun phonesAreNotScaledAndUseOneColumn() {
        assertEquals(1f, screenScale(390f, 844f))
        assertEquals(1f, screenScale(320f, 640f))
        assertEquals(1, lanesFor(390f, 844f))
    }

    @Test fun portraitTabletShowsThePhoneDesignEnlarged() {
        val k = screenScale(800f, 1280f)
        assertEquals(800f / 560f, k, 0.001f)
        assertEquals(560f, 800f / k, 0.5f)
        assertEquals(1, lanesFor(800f / k, 1280f / k))
    }

    @Test fun landscapeGetsTwoColumns() {
        assertEquals(2, lanesFor(844f, 390f))
        val k = screenScale(1280f, 800f)
        assertEquals(2, lanesFor(1280f / k, 800f / k))
        assertEquals(1.6f, screenScale(4000f, 2000f))
    }

    @Test fun textScaleIsClampedToAllowedSteps() {
        assertEquals(1f, sanitizeTextScale(null))
        assertEquals(1f, sanitizeTextScale(Float.NaN))
        assertEquals(1.3f, sanitizeTextScale(9f))
        assertEquals(0.9f, sanitizeTextScale(0.1f))
        assertEquals(1.15f, sanitizeTextScale(1.12f))
    }
}
