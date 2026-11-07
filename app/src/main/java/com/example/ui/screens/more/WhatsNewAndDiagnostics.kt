package com.example.ui.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.copyToClipboard
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.Changelog
import com.example.ui.theme.ChangelogEntry
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
private fun ChangelogPoints(entry: ChangelogEntry) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    entry.points(s.isArabic).forEach { p ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", style = MaterialTheme.typography.bodyMedium, color = c.primaryText)
            Text(p, style = MaterialTheme.typography.bodyMedium, color = c.onBackground, modifier = Modifier.weight(1f))
        }
    }
}

/** Shown once after each update (seen version ≠ installed version). */
@Composable
fun WhatsNewSheet(onTour: () -> Unit, onDone: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val entry = Changelog.latest
    ShareApkBottomSheet(onDismiss = onDone) {
        Text(s.whatsNewTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.onBackground)
        LtrBox { StatusBadge(s.whatsNewVersionFormat.format(entry.version), BadgeTone.Primary) }
        ChangelogPoints(entry)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(s.whatsNewTour, onTour, Modifier.weight(1f), icon = Icons.Outlined.Explore, height = ShareApkTokens.SheetButtonHeight)
            PrimaryButton(s.whatsNewDone, onDone, Modifier.weight(1f), height = ShareApkTokens.SheetButtonHeight)
        }
    }
}

/** Every release, newest first. */
@Composable
fun WhatsNewScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ScreenScaffold(title = s.whatsNewTitle, onBack = onBack, modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Changelog.entries, key = { it.version }) { e ->
                SurfaceCard(Modifier.fillMaxWidth()) {
                    LtrBox { Text(s.whatsNewVersionFormat.format(e.version), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.onBackground) }
                    ChangelogPoints(e)
                }
            }
        }
    }
}

@Composable
fun DiagnosticsScreen(lines: List<String>, onRefresh: () -> Unit, onClear: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    LaunchedEffect(Unit) { onRefresh() }
    ScreenScaffold(title = s.diagTitle, onBack = onBack, modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(s.diagHint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton(s.diagCopy, { copyToClipboard(context, "ShareAPK diagnostics", lines.joinToString("\n"), s.copiedToast) },
                            Modifier.weight(1f), icon = Icons.Outlined.ContentCopy, enabled = lines.isNotEmpty())
                        SecondaryButton(s.diagClear, onClear, Modifier.weight(1f), icon = Icons.Outlined.DeleteOutline, enabled = lines.isNotEmpty())
                    }
                }
            }
            if (lines.isEmpty()) item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(s.diagEmpty, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            items(lines) { line ->
                SurfaceCard(Modifier.fillMaxWidth()) {
                    LtrBox { Text(line, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = c.onBackground) }
                }
            }
        }
    }
}
