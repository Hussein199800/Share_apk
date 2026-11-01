package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.BrandMark
import com.example.ui.components.design.HeroCard
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

/** About page in the Seil/Sakina layout: brand, version, description, developer card, details. */
@Composable
fun AboutScreen(appVersion: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.aboutTitle, onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.SectionGap)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BrandMark(size = 76.dp)
                Text(s.appName, style = MaterialTheme.typography.headlineMedium, color = c.onBackground, textAlign = TextAlign.Center)
                LtrBox { Text(s.appVersionFormat.format(appVersion), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant) }
                Text(s.aboutAppDescription, style = MaterialTheme.typography.bodyMedium, color = c.onBackground, textAlign = TextAlign.Center)
            }

            HeroCard(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Person, null, Modifier.size(28.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        s.aboutDevPrefix + s.aboutDevName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(s.aboutDua, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                }
            }

            SurfaceCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Triple: label, value, value is Latin (render LTR so bidi doesn't scramble it).
                val rows = listOf(
                    Triple(s.aboutTechLabel, s.aboutTechValue, true),
                    Triple(s.aboutPrivacyLabel, s.aboutPrivacyValue, !s.isArabic),
                    Triple(s.aboutLibrariesLabel, s.aboutLibrariesValue, true),
                    Triple(s.aboutFontLabel, s.aboutFontValue, true)
                )
                rows.forEachIndexed { i, (k, v, latin) ->
                    if (i > 0) HorizontalDivider(color = c.outline)
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(k, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.weight(0.35f))
                        Text(
                            v,
                            style = MaterialTheme.typography.titleSmall.copy(
                                textDirection = if (latin) TextDirection.Ltr else TextDirection.Content,
                                // Keep visual alignment with the surrounding (RTL or LTR) layout.
                                textAlign = if (latin && s.isArabic) TextAlign.End else TextAlign.Start
                            ),
                            color = c.onBackground,
                            modifier = Modifier.weight(0.65f)
                        )
                    }
                }
            }
        }
    }
}
