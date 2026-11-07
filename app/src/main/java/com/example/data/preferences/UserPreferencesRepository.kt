package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FileNamingFormat
import com.example.data.model.LanguagePreference
import com.example.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.shareApkDataStore: DataStore<Preferences> by preferencesDataStore(name = "shareapk_user_prefs")

data class UserPreferencesState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val languagePreference: LanguagePreference = LanguagePreference.ARABIC,
    val fileNamingFormat: FileNamingFormat = FileNamingFormat.ORIGINAL_NAME,
    val autoClearCacheOnExit: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val favoritePackages: Set<String> = emptySet(),
    val lastBackupTreeUri: String? = null,
    /** Name shown to nearby devices; null = device model. */
    val deviceAlias: String? = null,
    val receiveEnabled: Boolean = true,
    /** Version whose "What's new" the user has seen; null on a fresh install. */
    val seenVersion: String? = null,
    /** Whole-UI text size: one of [TEXT_SCALES]. */
    val textScale: Float = 1f
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_pref")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("language_pref")
        val FILE_NAMING = stringPreferencesKey("file_naming_format")
        val AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache_exit")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val FAVORITES = stringSetPreferencesKey("favorite_packages")
        val LAST_BACKUP_URI = stringPreferencesKey("last_backup_tree_uri")
        val DEVICE_ALIAS = stringPreferencesKey("device_alias")
        val RECEIVE_ENABLED = booleanPreferencesKey("receive_enabled")
        val SEEN_VERSION = stringPreferencesKey("seen_version")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.shareApkDataStore.data.map { prefs ->
        UserPreferencesState(
            themePreference = prefs[Keys.THEME]?.let {
                runCatching { ThemePreference.valueOf(it) }.getOrNull()
            } ?: ThemePreference.SYSTEM,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
            languagePreference = prefs[Keys.LANGUAGE]?.let {
                runCatching { LanguagePreference.valueOf(it) }.getOrNull()
            } ?: LanguagePreference.ARABIC,
            fileNamingFormat = prefs[Keys.FILE_NAMING]?.let {
                runCatching { FileNamingFormat.valueOf(it) }.getOrNull()
            } ?: FileNamingFormat.ORIGINAL_NAME,
            autoClearCacheOnExit = prefs[Keys.AUTO_CLEAR_CACHE] ?: false,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
            favoritePackages = prefs[Keys.FAVORITES] ?: emptySet(),
            lastBackupTreeUri = prefs[Keys.LAST_BACKUP_URI],
            deviceAlias = prefs[Keys.DEVICE_ALIAS],
            receiveEnabled = prefs[Keys.RECEIVE_ENABLED] ?: true,
            seenVersion = prefs[Keys.SEEN_VERSION],
            // Clamp on read so a corrupt value can't break the layout.
            textScale = sanitizeTextScale(prefs[Keys.TEXT_SCALE])
        )
    }

    suspend fun setSeenVersion(version: String) {
        context.shareApkDataStore.edit { it[Keys.SEEN_VERSION] = version }
    }

    suspend fun setTextScale(scale: Float) {
        context.shareApkDataStore.edit { it[Keys.TEXT_SCALE] = sanitizeTextScale(scale) }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.shareApkDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.shareApkDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguagePreference(language: LanguagePreference) {
        context.shareApkDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setFileNamingFormat(format: FileNamingFormat) {
        context.shareApkDataStore.edit { it[Keys.FILE_NAMING] = format.name }
    }

    suspend fun setAutoClearCacheOnExit(enabled: Boolean) {
        context.shareApkDataStore.edit { it[Keys.AUTO_CLEAR_CACHE] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.shareApkDataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun toggleFavorite(packageName: String) {
        context.shareApkDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(packageName)) {
                current.remove(packageName)
            } else {
                current.add(packageName)
            }
            prefs[Keys.FAVORITES] = current
        }
    }

    suspend fun setLastBackupTreeUri(uriString: String?) {
        context.shareApkDataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(Keys.LAST_BACKUP_URI)
            } else {
                prefs[Keys.LAST_BACKUP_URI] = uriString
            }
        }
    }

    suspend fun setDeviceAlias(alias: String?) {
        context.shareApkDataStore.edit { prefs ->
            val clean = alias?.trim()?.take(40)
            if (clean.isNullOrEmpty()) prefs.remove(Keys.DEVICE_ALIAS) else prefs[Keys.DEVICE_ALIAS] = clean
        }
    }

    suspend fun setReceiveEnabled(enabled: Boolean) {
        context.shareApkDataStore.edit { it[Keys.RECEIVE_ENABLED] = enabled }
    }
}

val TEXT_SCALES = listOf(0.9f, 1f, 1.15f, 1.3f)

/** Snaps any stored value to the nearest allowed size (1 when missing or invalid). */
fun sanitizeTextScale(v: Float?): Float =
    if (v == null || v.isNaN()) 1f else TEXT_SCALES.minBy { kotlin.math.abs(it - v) }
