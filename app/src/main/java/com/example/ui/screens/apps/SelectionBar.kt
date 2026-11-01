package com.example.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

/** Floating multi-select bar shown above the tab bar. */
@Composable
fun SelectionBar(
    count: Int,
    totalBytes: Long,
    onClear: () -> Unit,
    onShare: () -> Unit,
    onZip: () -> Unit,
    onWifi: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.CardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("multi_select_action_bar")
            .shadow(14.dp, shape)
            .clip(shape)
            .background(c.surfaceElevated)
            .border(ShareApkTokens.Hairline, c.outline, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconActionButton(Icons.Outlined.Close, s.clearSelectionBtn, onClear)
            Text(
                s.selectedCountFormat.format(count, FileNameAndHashUtils.formatBytes(totalBytes)),
                style = MaterialTheme.typography.titleSmall,
                color = c.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconActionButton(Icons.Outlined.Share, s.shareSelectedBtn, onShare, Modifier.testTag("multi_share_selected_button"))
            IconActionButton(Icons.Outlined.FolderZip, s.backupSelectedZipBtn, onZip, Modifier.testTag("multi_backup_zip_button"))
            IconActionButton(Icons.Outlined.Wifi, s.wifiShareAction, onWifi, Modifier.testTag("multi_wifi_share_button"))
            PrimaryButton(
                text = s.navSend,
                onClick = onSend,
                icon = Icons.AutoMirrored.Outlined.Send,
                modifier = Modifier.weight(1f).testTag("selection_send_button")
            )
        }
    }
}
