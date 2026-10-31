package com.example.ui.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun UnknownSourcesPermissionDialog(onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(ShareApkGradients.indigo) { Icon(Icons.Outlined.Security, null, Modifier.size(24.dp)) }
            Text(s.unknownSourcesTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
        }
        Text(s.unknownSourcesBody, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(s.cancelBtn, onDismiss, Modifier.weight(1f), height = ShareApkTokens.SheetButtonHeight)
            PrimaryButton(s.grantPermissionBtn, onOpenSettings, Modifier.weight(2f), height = ShareApkTokens.SheetButtonHeight)
        }
    }
}
