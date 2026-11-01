package com.example.ui.components.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.ButtonRadius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.onBackground),
        cursorBrush = SolidColor(c.primaryText),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .height(ShareApkTokens.ButtonHeight)
                    .clip(shape)
                    .background(c.input)
                    .border(ShareApkTokens.Hairline, c.outline, shape)
                    .padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, maxLines = 1)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = LocalAppStrings.current.clearSearch,
                        tint = c.onSurfaceVariant,
                        modifier = Modifier
                            .size(ShareApkTokens.MinTouch)
                            .clickable(role = Role.Button) { onValueChange("") }
                            .padding(12.dp)
                    )
                }
            }
        }
    )
}

/** Plain single-line text field in the ShareAPK style. */
@Composable
fun TextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(ShareApkTokens.ButtonRadius)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.onBackground),
        cursorBrush = SolidColor(c.primaryText),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .height(ShareApkTokens.ButtonHeight)
                    .clip(shape)
                    .background(c.input)
                    .border(ShareApkTokens.Hairline, c.outline, shape)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, maxLines = 1)
                inner()
            }
        }
    )
}

/**
 * Horizontally scrolling chip row. The leading "all" chip maps to `null`.
 */
@Composable
fun <K> FilterChipRow(
    allLabel: String,
    options: List<Pair<K, String>>,
    selected: K?,
    onSelect: (K?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShareApkChip(allLabel, selected == null) { onSelect(null) }
        options.forEach { (key, label) -> ShareApkChip(label, selected == key) { onSelect(key) } }
    }
}

/** Single-choice chips (always one selected), wrapping onto new lines. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun <K> ChoiceChipRow(
    options: List<Pair<K, String>>,
    selected: K,
    onSelect: (K) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) -> ShareApkChip(label, selected == key) { onSelect(key) } }
    }
}

@Composable
private fun ShareApkChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val c = ShareApkTheme.colors
    val shape = RoundedCornerShape(18.dp)
    // 44dp touch target around a 36dp visual chip.
    Box(
        modifier = Modifier
            .height(ShareApkTokens.MinTouch)
            .semantics { selected = isSelected }
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(shape)
                .then(
                    if (isSelected) Modifier.background(ShareApkGradients.primary.brush(135f))
                    else Modifier.background(c.surface).border(ShareApkTokens.Hairline, c.outline, shape)
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else c.onBackground,
                maxLines = 1
            )
        }
    }
}
