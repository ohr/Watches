package org.ear.watches

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.ear.watches.ui.screens.MainStopwatchScreen
import org.ear.watches.ui.theme.WatchesTheme
import org.ear.watches.ui.viewmodel.StopwatchViewModel
import org.ear.watches.util.FeedbackManager

/**
 * The main entry point for the application. Sets up edge-to-edge display, initializes
 * dependencies like the ViewModel and FeedbackManager, and hosts the root Compose UI content.
 */
class MainActivity : AppCompatActivity() {

    /** Process-wide instance, shared with the foreground service (see [WatchesApplication]). */
    private val viewModel: StopwatchViewModel
        get() = (application as WatchesApplication).stopwatchViewModel

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Optional; timing works either way. */ }
    private var notificationPermissionRequested = false
    private lateinit var feedbackManager: FeedbackManager

    /** Volume-Down was pressed while nothing was running; a long press resets all watches. */
    private var volumeDownResetArmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Avoid flashing an empty list while persisted stopwatches are being restored.
        splashScreen.setKeepOnScreenCondition { !viewModel.isRestored.value }
        enableEdgeToEdge()
        feedbackManager = FeedbackManager(this)

        if (savedInstanceState == null) {
            handleIncomingFileIntent(intent)
        }
        // The language may have been changed in the system settings while we were away.
        viewModel.refreshLanguage()
        notificationPermissionRequested =
            savedInstanceState?.getBoolean(STATE_NOTIFICATION_PERMISSION_REQUESTED) ?: false

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            val anyRunning = uiState.stopwatches.any { it.isRunning }
            LaunchedEffect(anyRunning) {
                if (anyRunning && viewModel.uiState.value.settings.showRunningNotification) {
                    requestNotificationPermissionOnce()
                }
            }

            val shouldKeepScreenOn = uiState.settings.keepScreenOnWhileRunning && anyRunning
            LaunchedEffect(shouldKeepScreenOn) {
                if (shouldKeepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            WatchesTheme(
                themeMode = uiState.settings.themeMode,
                fontSizeScale = uiState.settings.fontSizeScale,
                dynamicColor = uiState.settings.useDynamicColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainStopwatchScreen(
                        viewModel = viewModel,
                        feedbackManager = feedbackManager,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingFileIntent(intent)
    }

    private fun handleIncomingFileIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            intent.data = null
            viewModel.loadBackupFromUri(
                context = applicationContext,
                uri = uri,
                onSuccess = { count ->
                    Toast.makeText(
                        this,
                        getString(R.string.import_success_toast, count),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onError = { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onUiStarted()
    }

    override fun onStop() {
        super.onStop()
        viewModel.onUiStopped()
        // The process may be killed at any time once in the background: flush pending changes.
        viewModel.persistNow()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_NOTIFICATION_PERMISSION_REQUESTED, notificationPermissionRequested)
    }

    /** Asks for the notification permission (Android 13+) the first time a watch is started. */
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationPermissionRequested) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return
        notificationPermissionRequested = true
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::feedbackManager.isInitialized) {
            feedbackManager.release()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = viewModel.uiState.value.settings
        if (!settings.hardwareVolumeKeysEnabled || event == null) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event.repeatCount == 0) {
                    val wasRunning = viewModel.uiState.value.stopwatches.any { it.isRunning }
                    viewModel.handleVolumeUp()
                    if (wasRunning) {
                        feedbackManager.playLap(settings.soundEnabled, settings.hapticsEnabled)
                    } else {
                        feedbackManager.playStart(settings.soundEnabled, settings.hapticsEnabled)
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.repeatCount == 0) {
                    volumeDownResetArmed = false
                    if (viewModel.handleVolumeDown()) {
                        feedbackManager.playStop(settings.soundEnabled, settings.hapticsEnabled)
                    } else {
                        volumeDownResetArmed = true
                    }
                } else if (volumeDownResetArmed &&
                    (event.isLongPress || event.eventTime - event.downTime >= ViewConfiguration.getLongPressTimeout())
                ) {
                    volumeDownResetArmed = false
                    if (viewModel.handleVolumeDownLongPress()) {
                        feedbackManager.playReset(settings.soundEnabled, settings.hapticsEnabled)
                    }
                }
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val settings = viewModel.uiState.value.settings

        if (settings.hardwareVolumeKeysEnabled) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> return true
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    if (volumeDownResetArmed) {
                        // Released before the long-press timeout: explain instead of resetting.
                        volumeDownResetArmed = false
                        if (viewModel.uiState.value.stopwatches.isNotEmpty()) {
                            Toast.makeText(this, R.string.hint_hold_volume_down_to_reset, Toast.LENGTH_SHORT).show()
                        }
                    }
                    return true
                }
            }
        }

        return super.onKeyUp(keyCode, event)
    }

    private companion object {
        const val STATE_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
}
