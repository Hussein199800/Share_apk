package com.example.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun UpdateSheet(
    state: UpdateState,
    currentVersion: String,
    onCheck: () -> Unit,
    onDownload: (ReleaseInfo) -> Unit,
    onInstall: (UpdateState.Ready) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(ShareApkGradients.rose) { Icon(Icons.Outlined.Sync, null, Modifier.size(24.dp)) }
            Column(Modifier.weight(1f)) {
                Text(s.updatesTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
                Text(s.appVersionFormat.format(currentVersion), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            }
        }
        val h = ShareApkTokens.SheetButtonHeight
        when (state) {
            UpdateState.Idle, UpdateState.Checking -> {
                Text(s.updateChecking, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
                GradientProgressBar(null)
            }
            UpdateState.UpToDate -> {
                StatusBadge(s.updateUpToDate.format(currentVersion), BadgeTone.Success)
                SecondaryButton(s.updateCheckBtn, onCheck, Modifier.fillMaxWidth(), height = h)
            }
            is UpdateState.Available -> {
                Text(s.updateAvailable.format(state.release.version), style = MaterialTheme.typography.titleSmall, color = c.primaryText)
                if (state.release.notes.isNotBlank()) {
                    SurfaceCard(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                        Text(state.release.notes, style = MaterialTheme.typography.bodySmall, color = c.onBackground, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                }
                PrimaryButton(s.updateDownloadBtn, { onDownload(state.release) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Download, height = h)
            }
            is UpdateState.Downloading -> {
                Text(s.updateDownloading, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
                GradientProgressBar(state.progress)
            }
            is UpdateState.Ready -> {
                StatusBadge(s.updateReady, BadgeTone.Success)
                PrimaryButton(s.updateInstallBtn, { onInstall(state) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.InstallMobile, height = h)
            }
            UpdateState.Unavailable -> {
                StatusBadge(s.updateUnavailable, BadgeTone.Info)
                SecondaryButton(s.updateCheckBtn, onCheck, Modifier.fillMaxWidth(), height = h)
            }
            is UpdateState.Failed -> {
                StatusBadge(s.updateFailed.format(state.reason), BadgeTone.Error)
                SecondaryButton(s.updateCheckBtn, onCheck, Modifier.fillMaxWidth(), height = h)
            }
        }
    }
}
