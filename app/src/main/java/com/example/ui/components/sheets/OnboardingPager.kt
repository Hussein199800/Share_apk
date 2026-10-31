package com.example.ui.components.sheets

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.HaloBackground
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.SecondaryButton
import com.example.ui.theme.GradientPair
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens
import kotlinx.coroutines.launch

private data class OnboardingPage(val icon: ImageVector, val gradient: GradientPair, val title: String, val body: String)

/** Full-screen first-run overlay. The last page explains the four tabs. */
@Composable
fun OnboardingPagerDialog(onFinishOnboarding: () -> Unit) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val pages = listOf(
        OnboardingPage(Icons.Outlined.FolderZip, ShareApkGradients.indigo, s.onboardingStep1Title, s.onboardingStep1Body),
        OnboardingPage(Icons.Outlined.Lock, ShareApkGradients.green, s.onboardingStep2Title, s.onboardingStep2Body),
        OnboardingPage(Icons.Outlined.Security, ShareApkGradients.amber, s.onboardingStep3Title, s.onboardingStep3Body),
        OnboardingPage(Icons.Outlined.Dashboard, ShareApkGradients.violet, s.onboardingTabsTitle, s.onboardingTabsBody)
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    BackHandler(onBack = onFinishOnboarding)
    // Drawn inside the activity window (not a Dialog) so system bar insets are applied correctly.
    run {
        HaloBackground(Modifier.pointerInput(Unit) {}) {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().padding(ShareApkTokens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s.appName, style = MaterialTheme.typography.titleMedium, color = c.onSurfaceVariant)
                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { i ->
                    val p = pages[i]
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                    ) {
                        GradientIconBox(p.gradient, size = 88.dp) { Icon(p.icon, null, Modifier.size(42.dp)) }
                        Text(p.title, style = MaterialTheme.typography.headlineMedium, color = c.onBackground, textAlign = TextAlign.Center)
                        Text(p.body, style = MaterialTheme.typography.bodyMedium, color = c.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    pages.indices.forEach { i ->
                        val active = i == pagerState.currentPage
                        Box(
                            Modifier
                                .animateContentSize()
                                .height(8.dp)
                                .width(if (active) 22.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (active) c.primaryText else c.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isLast) {
                        SecondaryButton(s.skipBtn, onFinishOnboarding, Modifier.weight(1f), height = ShareApkTokens.SheetButtonHeight)
                    }
                    PrimaryButton(
                        text = if (isLast) s.getStartedBtn else s.nextBtn,
                        onClick = {
                            if (isLast) onFinishOnboarding()
                            else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier.weight(1f).testTag("onboarding_next"),
                        height = ShareApkTokens.SheetButtonHeight
                    )
                }
            }
        }
    }
}
