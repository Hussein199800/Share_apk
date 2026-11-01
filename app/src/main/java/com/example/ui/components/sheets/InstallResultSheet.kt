package com.example.ui.components.sheets

import android.content.pm.PackageInstaller
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.installer.InstallSessionResult
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

fun installResultTone(result: InstallSessionResult): BadgeTone =
    if (result.status == PackageInstaller.STATUS_SUCCESS) BadgeTone.Success else BadgeTone.Error

@Composable
fun InstallStatusResultDialog(result: InstallSessionResult, onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val success = installResultTone(result) == BadgeTone.Success
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GradientIconBox(if (success) ShareApkGradients.green else ShareApkGradients.rose) {
                Icon(if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline, null, Modifier.size(24.dp))
            }
            Text(
                if (s.isArabic) result.statusTitleAr else result.statusTitleEn,
                style = MaterialTheme.typography.titleMedium,
                color = if (success) c.success else c.error
            )
        }
        Text(if (s.isArabic) result.messageAr else result.messageEn, style = MaterialTheme.typography.bodyMedium, color = c.onBackground)
        PrimaryButton(s.continueBtn, onDismiss, Modifier.fillMaxWidth(), height = ShareApkTokens.SheetButtonHeight)
    }
}
