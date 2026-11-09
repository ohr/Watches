package org.ear.watches.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.ear.watches.MainActivity
import org.ear.watches.R
import org.ear.watches.WatchesApplication
import org.ear.watches.data.util.SystemTimeProvider
import org.ear.watches.data.util.TimeFormatter
import org.ear.watches.ui.viewmodel.StopwatchViewModel

/**
 * Foreground service that runs while at least one stopwatch is running.
 *
 * It keeps the process alive during long sessions (so in-memory timing state is not lost when the
 * coach switches apps) and shows an ongoing notification with a live chronometer for the target
 * watch plus Lap / Stop / Stop all actions. It stops itself once no watch is running.
 */
class StopwatchService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timeProvider = SystemTimeProvider()
    private var currentModel: RunningNotificationModel? = null
    private var isInForeground = false

    private val viewModel: StopwatchViewModel
        get() = (application as WatchesApplication).stopwatchViewModel

    override fun onBind(intent: Intent?): IBinder? = null

    override fun attachBaseContext(base: Context) {
        // Android 13+ applies the per-app language to all contexts; older versions only to
        // AppCompat activities, so localize the notification texts explicitly.
        val locales = AppCompatDelegate.getApplicationLocales()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && !locales.isEmpty) {
            val config = Configuration(base.resources.configuration)
            config.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
            super.attachBaseContext(base.createConfigurationContext(config))
        } else {
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
        scope.launch {
            viewModel.uiState
                .map { RunningNotificationModel.from(it, timeProvider.currentTimeNanos(), timeProvider.currentTimeMillis()) }
                // Ignore millisecond jitter of the chronometer base caused by recomputing "now".
                .distinctUntilChanged { old, new -> old?.copy(chronometerBaseMillis = 0) == new?.copy(chronometerBaseMillis = 0) }
                .collect { model ->
                    currentModel = model
                    // Until onStartCommand has entered the foreground, it handles start/stop itself.
                    if (!isInForeground) return@collect
                    if (model == null) {
                        stopSelfCompletely()
                    } else {
                        NotificationManagerCompat.from(this@StopwatchService).let { manager ->
                            if (manager.areNotificationsEnabled()) {
                                @Suppress("MissingPermission") // Checked via areNotificationsEnabled().
                                manager.notify(NOTIFICATION_ID, buildNotification(model))
                            }
                        }
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must enter the foreground promptly after startForegroundService(), even if we stop right away.
        val model = currentModel ?: RunningNotificationModel.from(
            viewModel.uiState.value, timeProvider.currentTimeNanos(), timeProvider.currentTimeMillis()
        )
        if (model == null) {
            enterForeground(buildPlaceholderNotification())
            stopSelfCompletely()
            return START_NOT_STICKY
        }
        enterForeground(buildNotification(model))

        val watchId = intent?.getStringExtra(EXTRA_WATCH_ID)
        when (intent?.action) {
            ACTION_LAP -> watchId?.let { viewModel.lapStopwatch(it) }
            ACTION_STOP -> watchId?.let { viewModel.pauseStopwatch(it) }
            ACTION_STOP_ALL -> viewModel.pauseAll()
        }
        // State is in memory only; if the process dies, persistence restores it on next launch.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun enterForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        isInForeground = true
    }

    private fun stopSelfCompletely() {
        isInForeground = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun baseBuilder(): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_stopwatch)
            .setContentIntent(contentIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    }

    private fun buildPlaceholderNotification(): Notification =
        baseBuilder().setContentTitle(getString(R.string.app_name)).build()

    private fun buildNotification(model: RunningNotificationModel): Notification {
        val precision = viewModel.uiState.value.settings.timePrecision
        val separator = getString(R.string.subsecond_separator)
        val running = getString(R.string.notification_running_count, model.runningCount, model.totalCount)
        val text = if (model.lastLapNumber != null && model.lastLapMillis != null) {
            val lap = getString(
                R.string.notification_last_lap,
                model.lastLapNumber,
                TimeFormatter.formatTime(model.lastLapMillis, precision, separator)
            )
            "$running · $lap"
        } else {
            running
        }

        return baseBuilder()
            .setContentTitle(model.targetName)
            .setContentText(text)
            .setWhen(model.chronometerBaseMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .addAction(0, getString(R.string.notification_action_lap), actionIntent(ACTION_LAP, model.targetWatchId))
            .addAction(0, getString(R.string.notification_action_stop), actionIntent(ACTION_STOP, model.targetWatchId))
            .apply {
                if (model.runningCount > 1) {
                    addAction(0, getString(R.string.notification_action_stop_all), actionIntent(ACTION_STOP_ALL, null))
                }
            }
            .build()
    }

    private fun actionIntent(action: String, watchId: String?): PendingIntent {
        val intent = Intent(this, StopwatchService::class.java)
            .setAction(action)
            .putExtra(EXTRA_WATCH_ID, watchId)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val TAG = "StopwatchService"
        private const val CHANNEL_ID = "running_stopwatches"
        private const val NOTIFICATION_ID = 1

        const val ACTION_LAP = "org.ear.watches.action.LAP"
        const val ACTION_STOP = "org.ear.watches.action.STOP"
        const val ACTION_STOP_ALL = "org.ear.watches.action.STOP_ALL"
        private const val EXTRA_WATCH_ID = "watch_id"

        /** Starts the service; safe to call repeatedly. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, StopwatchService::class.java))
            } catch (e: IllegalStateException) {
                // E.g. ForegroundServiceStartNotAllowedException when started from the background
                // (Android 12+). Persistence still covers process death in that case.
                Log.w(TAG, "Could not start foreground service", e)
            }
        }

        private fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_running),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_running_desc)
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
