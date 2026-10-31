package com.example.ui.components.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

/** Bottom space screens leave for the floating tab bar; the root provides 0 when a rail is used. */
val LocalBottomBarClearance = compositionLocalOf<Dp> { ShareApkTokens.BottomBarClearance }

/** Screen header (title, subtitle, optional back and action) above [content]. Content is not scrolled. */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = ShareApkTheme.colors
    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ShareApkTokens.ScreenPadding, end = ShareApkTokens.ScreenPadding, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                IconActionButton(Icons.AutoMirrored.Outlined.ArrowBack, LocalAppStrings.current.navBack, onBack)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (action != null) action()
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = LocalBottomBarClearance.current),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareApkBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    allowSwipeDismiss: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = ShareApkTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { allowSwipeDismiss || it != SheetValue.Hidden }
        ),
        shape = RoundedCornerShape(topStart = ShareApkTokens.SheetRadius, topEnd = ShareApkTokens.SheetRadius),
        containerColor = c.surface,
        contentColor = c.onBackground,
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = ShareApkTokens.ScreenPadding, end = ShareApkTokens.ScreenPadding, bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}
