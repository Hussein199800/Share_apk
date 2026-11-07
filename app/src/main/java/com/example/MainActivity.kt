package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.design.AdaptiveScale
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ui.ShareApkAppRoot
import com.example.ui.viewmodel.ShareApkViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: ShareApkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingPackageIntent(intent)

        setContent {
            val prefs by viewModel.userPrefs.collectAsStateWithLifecycle()
            // Text size + tablet scale enlarge the whole UI; layout decisions use the scaled width.
            AdaptiveScale(prefs.textScale) { logicalWidth, _ ->
                ShareApkAppRoot(
                    viewModel = viewModel,
                    isExpandedScreen = logicalWidth >= 600.dp
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingPackageIntent(intent)
    }

    private fun handleIncomingPackageIntent(incomingIntent: Intent?) {
        if (incomingIntent == null) return
        when (incomingIntent.action) {
            Intent.ACTION_VIEW -> {
                incomingIntent.data?.let { uri ->
                    viewModel.inspectAndPrepareInstall(uri)
                }
            }
            Intent.ACTION_SEND -> {
                val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incomingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                streamUri?.let { uri ->
                    viewModel.inspectAndPrepareInstall(uri)
                }
            }
        }
    }
}
