package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FileNamingFormat
import com.example.data.model.LanguagePreference
import com.example.data.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shareapk_user_prefs")

data class UserPreferencesState(
    val themePreference: ThemePreference = ThemePreference.DARK,
    val dynamicColorEnabled: Boolean = false,
    val languagePreference: LanguagePreference = LanguagePreference.ARABIC,
    val fileNamingFormat: FileNamingFormat = FileNamingFormat.ORIGINAL_NAME,
    val autoClearCacheOnExit: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val favoritePackages: Set<String> = emptySet(),
    val lastBackupTreeUri: String? = null
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
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        UserPreferencesState(
            themePreference = prefs[Keys.THEME]?.let {
                runCatching { ThemePreference.valueOf(it) }.getOrNull()
            } ?: ThemePreference.DARK,
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
            lastBackupTreeUri = prefs[Keys.LAST_BACKUP_URI]
        )
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguagePreference(language: LanguagePreference) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setFileNamingFormat(format: FileNamingFormat) {
        context.dataStore.edit { it[Keys.FILE_NAMING] = format.name }
    }

    suspend fun setAutoClearCacheOnExit(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CLEAR_CACHE] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
    }

    suspend fun toggleFavorite(packageName: String) {
        context.dataStore.edit { prefs ->
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
        context.dataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(Keys.LAST_BACKUP_URI)
            } else {
                prefs[Keys.LAST_BACKUP_URI] = uriString
            }
        }
    }
}
