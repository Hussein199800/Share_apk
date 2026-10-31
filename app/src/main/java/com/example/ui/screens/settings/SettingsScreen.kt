package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.data.model.FileNamingFormat
import com.example.data.model.LanguagePreference
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferencesState
import com.example.ui.components.design.ChoiceChipRow
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.SurfaceCard
import com.example.ui.components.design.TextInputField
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
fun SettingsScreen(
    prefs: UserPreferencesState,
    cacheSizeBytes: Long,
    onThemeChange: (ThemePreference) -> Unit,
    onLanguageChange: (LanguagePreference) -> Unit,
    onNamingFormatChange: (FileNamingFormat) -> Unit,
    onClearCacheNow: () -> Unit,
    onAutoClearCacheChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    deviceAlias: String = "",
    deviceAliasPlaceholder: String = "",
    onDeviceAliasChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.tileSettingsTitle, onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)
        ) {
            Section(Icons.Outlined.Smartphone, s.deviceNameTitle) {
                // Local text is the source of truth while typing (the saved value is trimmed);
                // only adopt the stored alias when it first loads.
                var alias by remember { mutableStateOf(deviceAlias) }
                LaunchedEffect(deviceAlias) { if (alias.isEmpty()) alias = deviceAlias }
                TextInputField(alias, { alias = it.take(40); onDeviceAliasChange(alias) }, deviceAliasPlaceholder)
                Text(s.deviceNameHint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
            Section(Icons.Outlined.Palette, s.themeSectionTitle) {
                ChoiceChipRow(
                    listOf(
                        ThemePreference.SYSTEM to s.themeSystem,
                        ThemePreference.LIGHT to s.themeLight,
                        ThemePreference.DARK to s.themeDark
                    ),
                    prefs.themePreference, onThemeChange
                )
            }
            Section(Icons.Outlined.Language, s.languageSectionTitle) {
                ChoiceChipRow(
                    listOf(
                        LanguagePreference.ARABIC to "العربية (RTL)",
                        LanguagePreference.ENGLISH to "English (LTR)",
                        LanguagePreference.SYSTEM to s.themeSystem
                    ),
                    prefs.languagePreference, onLanguageChange
                )
            }
            Section(Icons.Outlined.TextFields, s.namingSectionTitle) {
                ChoiceChipRow(
                    listOf(
                        FileNamingFormat.ORIGINAL_NAME to s.namingOriginal,
                        FileNamingFormat.LATIN_TRANSLITERATION to s.namingLatin,
                        FileNamingFormat.PACKAGE_NAME to s.namingPackage
                    ),
                    prefs.fileNamingFormat, onNamingFormatChange
                )
            }
            Section(Icons.Outlined.Storage, s.cacheSectionTitle, trailing = {
                LtrBox { Text(FileNameAndHashUtils.formatBytes(cacheSizeBytes), style = MaterialTheme.typography.labelLarge, color = c.primaryText) }
            }) {
                SecondaryButton(s.clearCacheNowBtn, onClearCacheNow, Modifier.fillMaxWidth(), icon = Icons.Outlined.DeleteSweep)
                // Whole row is the toggle, so TalkBack reads the label with the switch.
                Row(
                    Modifier.toggleable(
                        value = prefs.autoClearCacheOnExit,
                        role = Role.Switch,
                        onValueChange = onAutoClearCacheChange
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(s.autoClearCacheToggle, style = MaterialTheme.typography.bodyMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    Switch(
                        checked = prefs.autoClearCacheOnExit,
                        onCheckedChange = null,
                        colors = SwitchDefaults.colors(checkedTrackColor = c.primary)
                    )
                }
            }
        }
    }
}

@Composable
internal fun Section(
    icon: ImageVector,
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val c = ShareApkTheme.colors
    SurfaceCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = c.primaryText, modifier = Modifier.size(20.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.onBackground, modifier = Modifier.weight(1f))
            if (trailing != null) trailing()
        }
        content()
    }
}
