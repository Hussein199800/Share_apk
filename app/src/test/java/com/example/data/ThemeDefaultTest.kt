package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.shareApkDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeDefaultTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun themePrefsBehaveAcrossDefaultSavedAndLegacyKeys() = runTest {
        // One test so the process-wide DataStore singleton is exercised in a known order.
        val repo = UserPreferencesRepository(context)
        assertEquals(ThemePreference.SYSTEM, repo.preferencesFlow.first().themePreference)

        repo.setThemePreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, repo.preferencesFlow.first().themePreference)

        // Legacy dynamic-color key from older versions must not break reading prefs.
        context.shareApkDataStore.edit { it[booleanPreferencesKey("dynamic_color")] = true }
        assertEquals(ThemePreference.DARK, repo.preferencesFlow.first().themePreference)
    }

    @Test
    fun receivePrefsDefaultsAndAlias() = runTest {
        val repo = UserPreferencesRepository(context)
        val p = repo.preferencesFlow.first()
        org.junit.Assert.assertTrue(p.receiveEnabled)
        org.junit.Assert.assertNull(p.deviceAlias)
        repo.setDeviceAlias("  هاتف حسين  ")
        assertEquals("هاتف حسين", repo.preferencesFlow.first().deviceAlias)
        repo.setDeviceAlias(" ")
        org.junit.Assert.assertNull(repo.preferencesFlow.first().deviceAlias)
        repo.setReceiveEnabled(false)
        org.junit.Assert.assertFalse(repo.preferencesFlow.first().receiveEnabled)
    }
}
