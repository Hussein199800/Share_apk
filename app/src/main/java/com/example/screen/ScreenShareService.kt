package com.example.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.receive.localLanIpv4
import com.example.ui.theme.ArabicStrings
import com.example.ui.theme.EnglishStrings
import com.example.web.PinGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

/** What the Screen tab shows about this phone's own sharing. */
data class ScreenShareState(
    val running: Boolean = false,
    val address: String = "",
    val pin: String = "",
    /** Name of the connected helper, null while nobody is watching. */
    val viewer: String? = null,
    val error: String? = null
)

/** Process-wide sharing state, written by [ScreenShareService]. */
object ScreenShare {
    internal val _state = MutableStateFlow(ScreenShareState())
    val state: StateFlow<ScreenShareState> = _state.asStateFlow()
    /** Set by the service; the overlay (pointer/drawing) listens to it. */
    @Volatile internal var eventSink: ((ScreenEvent) -> Unit)? = null
}

/**
 * Foreground service of type mediaProjection: started right after the user's capture consent,
 * it owns the projection, the encoder and the [ScreenServer]. The notification's button stops it.
 */
class ScreenShareService : Service() {
    private var capture: ScreenCapture? = null
    private var server: ScreenServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        val resultCode = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra(EXTRA_DATA)
        // Android 14+: be in the foreground with type mediaProjection before getMediaProjection().
        startForeground(NOTIFICATION_ID, notification(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        if (data == null || capture != null) return START_NOT_STICKY
        runCatching {
            val mpm = getSystemService(MediaProjectionManager::class.java)
            val projection = mpm.getMediaProjection(resultCode, data) ?: error("no projection")
            val metrics = resources.displayMetrics
            val gate = PinGate(onPinChanged = { pin -> ScreenShare._state.update { it.copy(pin = pin) } })
            lateinit var cap: ScreenCapture
            val srv = ScreenServer(
                gate,
                size = { cap.width to cap.height },
                onViewer = { name ->
                    ScreenShare._state.update { it.copy(viewer = name) }
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(name))
                },
                onEvent = { e -> ScreenShare.eventSink?.invoke(e) },
                requestKeyFrame = { cap.requestKeyFrame() }
            )
            cap = ScreenCapture(projection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                onFrame = srv::offer, onStopped = { stopSelf() })
            cap.start()
            val port = srv.start(ScreenProtocol.PORT)
            capture = cap; server = srv
            ScreenShare._state.value = ScreenShareState(running = true, address = "${localLanIpv4() ?: "?"}:$port", pin = gate.pin)
            ScreenOverlay.show(this)
        }.onFailure { e ->
            ScreenShare._state.value = ScreenShareState(error = e.message ?: e.javaClass.simpleName)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ScreenOverlay.hide()
        runCatching { server?.stop() }
        runCatching { capture?.stop() }
        server = null; capture = null
        ScreenShare._state.update { ScreenShareState(error = it.error) }
        super.onDestroy()
    }

    private fun notification(viewer: String?): Notification {
        val s = if (Locale.getDefault().language == "ar") ArabicStrings else EnglishStrings
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, s.screenChannelName, NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, ScreenShareService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(if (viewer != null) s.screenWatchingFormat.format(viewer) else s.screenSharingNotif)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, s.screenStopBtn, stop)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "shareapk_screen_channel"
        private const val NOTIFICATION_ID = 4712
        private const val EXTRA_CODE = "code"
        private const val EXTRA_DATA = "data"
        private const val ACTION_STOP = "com.example.screen.STOP"

        fun start(context: Context, resultCode: Int, data: Intent) {
            ScreenShare._state.value = ScreenShareState()
            context.startForegroundService(
                Intent(context, ScreenShareService::class.java).putExtra(EXTRA_CODE, resultCode).putExtra(EXTRA_DATA, data)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenShareService::class.java))
        }
    }
}
