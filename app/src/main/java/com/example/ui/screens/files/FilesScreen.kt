package com.example.ui.screens.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.send.PickedItem
import com.example.ui.components.design.FileTypeIcon
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.ShareApkBottomSheet
import com.example.ui.components.design.SurfaceCard
import com.example.ui.theme.GradientPair
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import com.example.util.FileNameAndHashUtils

@Composable
fun FilesScreen(
    items: List<PickedItem>,
    onPickFile: () -> Unit,
    onPickMedia: () -> Unit,
    onPickFolder: () -> Unit,
    onAddText: (String) -> Unit,
    onPaste: () -> Unit,
    onRemove: (PickedItem) -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    var showText by remember { mutableStateOf(false) }
    ScreenScaffold(title = s.navFiles, subtitle = s.filesSubtitle, modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)) {
                        PickTile(Icons.Outlined.InsertDriveFile, s.pickFile, ShareApkGradients.indigo, onPickFile, Modifier.weight(1f).testTag("pick_file"))
                        PickTile(Icons.Outlined.Image, s.pickMedia, ShareApkGradients.violet, onPickMedia, Modifier.weight(1f))
                        PickTile(Icons.Outlined.Folder, s.pickFolder, ShareApkGradients.amber, onPickFolder, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(ShareApkTokens.CardGap)) {
                        PickTile(Icons.Outlined.Notes, s.pickText, ShareApkGradients.sky, { showText = true }, Modifier.weight(1f).testTag("pick_text"))
                        PickTile(Icons.Outlined.ContentPaste, s.pickPaste, ShareApkGradients.green, onPaste, Modifier.weight(1f))
                        Column(Modifier.weight(1f)) {}
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.selectedItemsTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    if (items.isNotEmpty()) SecondaryButton(s.clearAllBtn, onClear)
                }
            }
            if (items.isEmpty()) item {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(s.filesEmpty, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
            items(items, key = { it.id }) { item ->
                SurfaceCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FileTypeIcon(if (item.isText) "x.txt" else item.name, item.mimeType, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.text ?: item.name, style = MaterialTheme.typography.bodyLarge, color = c.onBackground,
                                maxLines = if (item.isText) 2 else 1, overflow = TextOverflow.Ellipsis
                            )
                            if (!item.isText) LtrBox {
                                Text(FileNameAndHashUtils.formatBytes(item.size), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                            }
                        }
                        IconActionButton(Icons.Outlined.Close, s.deleteItemAction, { onRemove(item) })
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            PrimaryButton(
                s.sendToDeviceBtn + "  ·  " + s.itemsSummary.format(items.size, FileNameAndHashUtils.formatBytes(items.sumOf { it.size })),
                onSend,
                Modifier.fillMaxWidth().padding(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp).testTag("files_send"),
                icon = Icons.AutoMirrored.Outlined.Send
            )
        }
    }
    if (showText) TextSheet(onAdd = { onAddText(it); showText = false }, onDismiss = { showText = false })
}

@Composable
private fun PickTile(icon: ImageVector, label: String, gradient: GradientPair, onClick: () -> Unit, modifier: Modifier) {
    val c = ShareApkTheme.colors
    SurfaceCard(modifier.heightIn(min = 96.dp), onClick = onClick, verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        GradientIconBox(gradient, Modifier.align(Alignment.CenterHorizontally), size = 40.dp) { Icon(icon, null, Modifier.size(20.dp)) }
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.onBackground, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TextSheet(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    var text by remember { mutableStateOf("") }
    ShareApkBottomSheet(onDismiss = onDismiss) {
        Text(s.textSheetTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground)
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text(s.textHint) },
            minLines = 3, maxLines = 8,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(ShareApkTokens.ButtonRadius),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.primaryText, unfocusedBorderColor = c.outline),
            modifier = Modifier.fillMaxWidth().testTag("text_input")
        )
        PrimaryButton(s.addBtn, { onAdd(text) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank(), height = ShareApkTokens.SheetButtonHeight)
    }
}
