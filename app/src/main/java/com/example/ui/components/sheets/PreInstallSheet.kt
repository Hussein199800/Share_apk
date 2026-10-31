package com.example.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PreInstallValidationInfo
import com.example.ui.components.copyToClipboard
import com.example.ui.components.design.AppIcon
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientProgressBar
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
fun PreInstallValidationSheet(
    validation: PreInstallValidationInfo,
    isCommitting: Boolean,
    onConfirmInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    val mono = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)

    ShareApkBottomSheet(onDismiss = onDismiss) {
        Text(s.installerSheetTitle, style = MaterialTheme.typography.titleLarge, color = c.onBackground)
        StatusBadge(
            validation.securityWarning ?: s.securityVerifiedBadge,
            if (validation.isValid) BadgeTone.Success else BadgeTone.Error,
            icon = if (validation.isValid) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline
        )
        SurfaceCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(validation.appLabel, validation.iconBitmap, size = 52.dp)
                Column(Modifier.weight(1f)) {
                    Text(validation.appLabel, style = MaterialTheme.typography.titleMedium, color = c.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    LtrBox { Text(validation.packageName, style = mono, color = c.onSurfaceVariant) }
                }
            }
            LtrBox {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("File: ${validation.fileName} (${if (validation.isApksBundle) "Split .APKS Bundle" else "Single .APK"})", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Text("Version: v${validation.versionName} (${validation.versionCode}) • minSdk ${validation.minSdk} / targetSdk ${validation.targetSdk}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    Text("Size: ${FileNameAndHashUtils.formatBytes(validation.fileSizeBytes)} • Modules: ${validation.splitEntryNames.size}", style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                }
            }
            if (validation.sha256Hash.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LtrBox { Text("SHA-256: ${validation.sha256Hash.take(24)}...", style = mono, color = c.primaryText) }
                    Spacer(Modifier.weight(1f))
                    IconActionButton(Icons.Outlined.ContentCopy, s.copyAction, {
                        copyToClipboard(context, "SHA-256", validation.sha256Hash, s.copiedToast)
                    })
                }
            }
        }
        if (isCommitting) {
            GradientProgressBar(progress = null)
            Text(s.installingProgress, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(s.cancelBtn, onDismiss, Modifier.weight(1f), height = ShareApkTokens.SheetButtonHeight)
            PrimaryButton(
                s.installNowBtn, onConfirmInstall,
                Modifier.weight(2f).testTag("confirm_install_button"),
                icon = Icons.Outlined.InstallMobile,
                enabled = validation.isValid && !isCommitting,
                height = ShareApkTokens.SheetButtonHeight
            )
        }
    }
}
