package com.example.ui.components.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

/** Collapsible how-to card: numbered steps plus optional tips. */
@Composable
fun HelpCard(
    title: String,
    steps: List<String>,
    modifier: Modifier = Modifier,
    tips: List<String> = emptyList(),
    initiallyExpanded: Boolean = false
) {
    val c = ShareApkTheme.colors
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    SurfaceCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = ShareApkTokens.MinTouch)
                .clickable(role = Role.Button) { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradientIconBox(ShareApkGradients.sky, size = 36.dp) { Icon(Icons.Outlined.HelpOutline, null, Modifier.size(20.dp)) }
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.onBackground, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = c.onSurfaceVariant)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                steps.forEachIndexed { i, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape).background(ShareApkGradients.primary.brush(135f)),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    }
                }
                if (tips.isNotEmpty()) {
                    Text(LocalAppStrings.current.helpTipsTitle, style = MaterialTheme.typography.labelLarge, color = c.primaryText)
                    tips.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", color = c.primaryText)
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
