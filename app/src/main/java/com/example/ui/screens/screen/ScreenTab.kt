package com.example.ui.screens.screen

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.provider.Settings
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ScreenShare
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.receive.discovery.NearbyDevice
import com.example.screen.ScreenEvent
import com.example.screen.ScreenOverlay
import com.example.screen.ScreenShareService
import com.example.screen.ScreenShareState
import com.example.screen.ScreenViewer
import com.example.screen.ViewerState
import com.example.ui.components.design.AdaptiveList
import com.example.ui.components.design.BadgeTone
import com.example.ui.components.design.ChoiceChipRow
import com.example.ui.components.design.GradientIconBox
import com.example.ui.components.design.IconActionButton
import com.example.ui.components.design.PrimaryButton
import com.example.ui.components.design.ScreenScaffold
import com.example.ui.components.design.SecondaryButton
import com.example.ui.components.design.StatusBadge
import com.example.ui.components.design.SurfaceCard
import com.example.ui.components.design.TextInputField
import com.example.ui.components.design.fullLine
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LtrBox
import com.example.ui.theme.ShareApkGradients
import com.example.ui.theme.ShareApkTheme
import com.example.ui.theme.ShareApkTokens

@Composable
fun ScreenTab(
    share: ScreenShareState,
    peers: List<NearbyDevice>,
    sharingHosts: Set<String>,
    viewerState: ViewerState,
    viewer: ScreenViewer,
    onProbe: () -> Unit,
    onWatch: (address: String, pin: String) -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalAppStrings.current
    val c = ShareApkTheme.colors
    val context = LocalContext.current
    var canOverlay by remember { mutableStateOf(ScreenOverlay.canDraw(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { canOverlay = ScreenOverlay.canDraw(context) }
    LaunchedEffect(Unit) { onProbe() }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data
        if (r.resultCode == Activity.RESULT_OK && data != null) ScreenShareService.start(context, r.resultCode, data)
    }
    var address by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }

    ScreenScaffold(title = s.navScreen, subtitle = s.screenSubtitle, modifier = modifier) {
        AdaptiveList(
            contentPadding = PaddingValues(horizontal = ShareApkTokens.ScreenPadding, vertical = 8.dp),
            gap = ShareApkTokens.CardGap,
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- This phone shares its screen ----
            item(key = "share") {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientIconBox(ShareApkGradients.violet) { Icon(Icons.Outlined.ScreenShare, null, Modifier.size(24.dp)) }
                        Text(if (share.running) s.screenSharingTitle else s.screenShareCardTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    }
                    if (!share.running) {
                        Text(s.screenShareCardHint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        share.error?.let { StatusBadge(it, BadgeTone.Error) }
                        PrimaryButton(s.screenShareCardTitle, {
                            val mpm = context.getSystemService(MediaProjectionManager::class.java)
                            consent.launch(mpm.createScreenCaptureIntent())
                        }, Modifier.fillMaxWidth().testTag("screen_share_button"), icon = Icons.Outlined.ScreenShare)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.screenCodeLabel, style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                            LtrBox { Text(share.pin, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = 8.sp, color = c.onBackground) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.screenAddressLabel, style = MaterialTheme.typography.titleSmall, color = c.onSurfaceVariant, modifier = Modifier.weight(1f))
                            LtrBox { Text(share.address, style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Monospace), color = c.primaryText) }
                        }
                        StatusBadge(share.viewer?.let { s.screenWatchingFormat.format(it) } ?: s.screenWaiting, if (share.viewer != null) BadgeTone.Success else BadgeTone.Info)
                        Text(s.screenSecureNote, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                        PrimaryButton(s.screenStopBtn, { ScreenShareService.stop(context) }, Modifier.fillMaxWidth(), icon = Icons.Outlined.StopCircle)
                    }
                }
            }
            if (!canOverlay) item(key = "overlay") {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(s.screenOverlayTitle, style = MaterialTheme.typography.titleSmall, color = c.onBackground)
                    Text(s.screenOverlayHint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    SecondaryButton(s.screenOverlayBtn, {
                        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    }, Modifier.fillMaxWidth())
                }
            }

            // ---- This phone helps another ----
            fullLine(key = "watchTitle") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.screenWatchTitle, style = MaterialTheme.typography.titleMedium, color = c.onBackground, modifier = Modifier.weight(1f))
                    IconActionButton(Icons.Outlined.Refresh, s.refreshAction, onProbe)
                }
            }
            item(key = "watch") {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Text(s.screenWatchHint, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    val sharing = peers.filter { it.ip in sharingHosts }
                    if (sharing.isEmpty()) {
                        Text(s.screenNoDevices, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
                    } else {
                        ChoiceChipRow(sharing.map { it.ip to it.info.alias }, address.substringBefore(':'), onSelect = { address = it })
                    }
                    TextInputField(address, { address = it.trim().take(60) }, s.screenAddressHint)
                    TextInputField(pin, { v -> pin = v.filter { it.isDigit() }.take(4) }, s.screenPinHint)
                    when (viewerState) {
                        ViewerState.WrongPin -> StatusBadge(s.screenWrongPin, BadgeTone.Error)
                        ViewerState.Ended -> StatusBadge(s.screenEnded, BadgeTone.Warning)
                        is ViewerState.Failed -> StatusBadge(s.errorReasons.getValue(com.example.util.ErrorClassifier.classify(viewerState.message)), BadgeTone.Error)
                        else -> Unit
                    }
                    PrimaryButton(s.screenWatchBtn, { onWatch(address, pin) }, Modifier.fillMaxWidth().testTag("screen_watch_button"),
                        icon = Icons.Outlined.Visibility, enabled = address.isNotBlank() && pin.length == 4)
                }
            }
        }
    }

    if (viewerState is ViewerState.Connecting || viewerState is ViewerState.Watching) {
        ViewerDialog(viewerState, viewer, onLeave)
    }
}

/** Full-screen view of the other phone; touches become a pointer or a drawing on their screen. */
@Composable
private fun ViewerDialog(state: ViewerState, viewer: ScreenViewer, onLeave: () -> Unit) {
    val s = LocalAppStrings.current
    var drawMode by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onLeave, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(ShareApkTheme.colors.background).safeDrawingPadding()) {
            if (state is ViewerState.Watching) {
                Box(Modifier.fillMaxSize().padding(bottom = 72.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .aspectRatio(state.width.toFloat() / state.height)
                            .pointerInput(drawMode) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    fun send(x: Float, y: Float) {
                                        val fx = (x / size.width).coerceIn(0f, 1f); val fy = (y / size.height).coerceIn(0f, 1f)
                                        viewer.send(if (drawMode) ScreenEvent.Draw(fx, fy) else ScreenEvent.Point(fx, fy))
                                    }
                                    send(down.position.x, down.position.y)
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull() ?: break
                                        if (!ch.pressed) break
                                        send(ch.position.x, ch.position.y)
                                        ch.consume()
                                    }
                                    if (drawMode) viewer.send(ScreenEvent.End)
                                }
                            }
                    ) {
                        AndroidView(factory = { ctx ->
                            SurfaceView(ctx).apply {
                                holder.addCallback(object : SurfaceHolder.Callback {
                                    override fun surfaceCreated(h: SurfaceHolder) = viewer.setSurface(h.surface)
                                    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) = Unit
                                    override fun surfaceDestroyed(h: SurfaceHolder) = viewer.setSurface(null)
                                })
                            }
                        }, modifier = Modifier.fillMaxSize())
                    }
                }
            } else {
                Text(s.screenConnecting, color = ShareApkTheme.colors.onBackground, modifier = Modifier.align(Alignment.Center))
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    ChoiceChipRow(listOf(false to s.screenModePoint, true to s.screenModeDraw), drawMode, onSelect = { drawMode = it })
                }
                PrimaryButton(s.screenLeave, onLeave, icon = Icons.Outlined.Close)
            }
        }
    }
}
