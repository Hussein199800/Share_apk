package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun AboutScreen(appVersion: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    ScreenScaffold(title = s.aboutTitle, subtitle = s.appName, onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)
        ) {
            HeroCard(Modifier.fillMaxWidth()) {
                GradientIconBox(ShareApkGradients.violet, size = 52.dp) { Icon(Icons.Outlined.Code, null) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(s.aboutDeveloperHeader, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                    Text(s.developerNamePrimary, style = MaterialTheme.typography.titleSmall)
                    Text(s.developerNameSecondary, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(s.developerRoleBio, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
                StatusBadge(s.offlinePrivacyBadge, BadgeTone.Success, icon = Icons.Outlined.Security)
                LtrBox {
                    Text(
                        "${s.appVersionFormat.format(appVersion)} • minSdk 31 (Android 12–15+)",
                        style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant
                    )
                }
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Text(s.openSourceLicenses, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                Text(s.fontLicenseLine, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                SecondaryButton(
                    s.repoTransparencyLabel,
                    {
                        val repoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/shareapk-android/shareapk"))
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        runCatching { context.startActivity(repoIntent) }
                    },
                    Modifier.fillMaxWidth().testTag("project_repository_link_button"),
                    icon = Icons.AutoMirrored.Outlined.OpenInNew
                )
            }
        }
    }
}
