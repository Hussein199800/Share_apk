package com.example.screen

/**
 * The Screen tab is hidden: same-Wi-Fi only isn't useful enough; it returns when it works across
 * networks (WebRTC + relay, planned). To re-enable: set ENABLED, and restore the manifest's
 * FOREGROUND_SERVICE_MEDIA_PROJECTION / SYSTEM_ALERT_WINDOW permissions and ScreenShareService.
 */
object ScreenFeature {
    const val ENABLED = false
}
