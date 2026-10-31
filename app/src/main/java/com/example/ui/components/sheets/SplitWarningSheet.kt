package com.example.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun SplitApkWarningDialog(
    appLabel: String,
    splitCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(ShareApkGradients.amber) { Icon(Icons.Outlined.FolderZip, null, Modifier.size(24.dp)) }
            Text("${s.splitWarningTitle} — $appLabel", style = MaterialTheme.typography.titleMedium, color = c.onBackground)
        }
        Text(s.splitWarningBody, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
        LtrBox {
            Text(
                "base.apk + $splitCount split_*.apk → .apks ZIP bundle",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = c.primaryText
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(s.cancelBtn, onDismiss, Modifier.weight(1f), height = ShareApkTokens.SheetButtonHeight)
            PrimaryButton(s.continueBtn, onConfirm, Modifier.weight(2f), height = ShareApkTokens.SheetButtonHeight)
        }
    }
}
