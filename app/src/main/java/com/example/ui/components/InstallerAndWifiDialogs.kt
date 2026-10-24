package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.PreInstallValidationInfo
import com.example.installer.InstallSessionResult
import com.example.ui.theme.BurnishedGold
import com.example.ui.theme.EmeraldVerifiedDark
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.util.FileNameAndHashUtils
import com.example.wifi.WifiServerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreInstallValidationSheet(
    validation: PreInstallValidationInfo,
    isCommitting: Boolean,
    onConfirmInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (validation.isValid) Icons.Default.Security else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (validation.isValid) EmeraldVerifiedDark else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.installerSheetTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Security Verification Status Banner
            Surface(
                color = if (validation.isValid) {
                    EmeraldVerifiedDark.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (validation.isValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (validation.isValid) EmeraldVerifiedDark else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = validation.securityWarning ?: strings.securityVerifiedBadge,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (validation.isValid) EmeraldVerifiedDark else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // App Summary Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (validation.iconBitmap != null) {
                                Image(
                                    bitmap = validation.iconBitmap.asImageBitmap(),
                                    contentDescription = validation.appLabel,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = BurnishedGold,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = validation.appLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            LtrBox {
                                Text(
                                    text = validation.packageName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LtrBox {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "File: ${validation.fileName} (${if (validation.isApksBundle) "Split .APKS Bundle" else "Single .APK"})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Version: v${validation.versionName} (${validation.versionCode}) • minSdk ${validation.minSdk} / targetSdk ${validation.targetSdk}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Size: ${FileNameAndHashUtils.formatBytes(validation.fileSizeBytes)} • Modules: ${validation.splitEntryNames.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (validation.sha256Hash.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LtrBox {
                                Text(
                                    text = "SHA-256: ${validation.sha256Hash.take(24)}...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = BurnishedGold
                                )
                            }
                            IconButton(
                                onClick = {
                                    copyToClipboard(context, "SHA-256", validation.sha256Hash, strings.copiedToast)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = strings.copyAction,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isCommitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = strings.installingProgress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurnishedGold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.cancelBtn)
                }

                Button(
                    onClick = onConfirmInstall,
                    enabled = validation.isValid && !isCommitting,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_install_session_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurnishedGold,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.installNowBtn, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SplitApkWarningDialog(
    appLabel: String,
    splitCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FolderZip,
                contentDescription = null,
                tint = BurnishedGold,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = "${strings.splitWarningTitle} — $appLabel")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = strings.splitWarningBody)
                LtrBox {
                    Text(
                        text = "base.apk + $splitCount split_*.apk → .apks ZIP bundle",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        color = BurnishedGold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurnishedGold,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("confirm_split_warning_button")
            ) {
                Text(strings.continueBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn)
            }
        }
    )
}

@Composable
fun UnknownSourcesPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Security, contentDescription = null, tint = BurnishedGold)
        },
        title = { Text(strings.unknownSourcesTitle) },
        text = { Text(strings.unknownSourcesBody) },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurnishedGold,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(strings.grantPermissionBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn)
            }
        }
    )
}

@Composable
fun InstallStatusResultDialog(
    result: InstallSessionResult,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isSuccess = result.status == android.content.pm.PackageInstaller.STATUS_SUCCESS
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (isSuccess) EmeraldVerifiedDark else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(if (strings.isArabic) result.statusTitleAr else result.statusTitleEn)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (strings.isArabic) result.messageAr else result.messageEn)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.continueBtn)
            }
        }
    )
}

@Composable
fun WifiShareServerDialog(
    state: WifiServerState,
    onStopServer: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60

    AlertDialog(
        onDismissRequest = onStopServer,
        icon = {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = BurnishedGold, modifier = Modifier.size(30.dp))
        },
        title = {
            Text(strings.wifiServerTitle, textAlign = TextAlign.Center)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.wifiServerSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                state.qrBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, BurnishedGold, RoundedCornerShape(16.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Wi-Fi QR Code",
                            modifier = Modifier.size(196.dp)
                        )
                    }
                }

                // Local URL Box with one-tap copy
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LtrBox {
                            Text(
                                text = state.url,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                color = BurnishedGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "Wi-Fi Server URL", state.url, strings.copiedToast)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = strings.copyAction)
                        }
                    }
                }

                // 5-Minute Auto-Stop Countdown Badge
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.autoStopCountdown,
                            style = MaterialTheme.typography.labelLarge
                        )
                        LtrBox {
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                                color = BurnishedGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStopServer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stop_wifi_server_button")
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.stopServerBtn, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OnboardingPagerDialog(
    onFinishOnboarding: () -> Unit
) {
    val strings = LocalAppStrings.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onFinishOnboarding,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, tint = BurnishedGold)
                Text(strings.appName, style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val (icon, title, body) = when (page) {
                        0 -> Triple(
                            Icons.Default.FolderZip,
                            strings.onboardingStep1Title,
                            strings.onboardingStep1Body
                        )
                        1 -> Triple(
                            Icons.Default.Lock,
                            strings.onboardingStep2Title,
                            strings.onboardingStep2Body
                        )
                        else -> Triple(
                            Icons.Default.Security,
                            strings.onboardingStep3Title,
                            strings.onboardingStep3Body
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = BurnishedGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { idx ->
                        val active = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .size(if (active) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) BurnishedGold
                                    else MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinishOnboarding()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurnishedGold,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (pagerState.currentPage < 2) strings.nextBtn else strings.getStartedBtn,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (pagerState.currentPage < 2) {
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(strings.getStartedBtn)
                }
            }
        }
    )
}
