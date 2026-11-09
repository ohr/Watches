package org.ear.watches

import android.Manifest
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.ui.viewmodel.StopwatchViewModel
import org.ear.watches.util.StopwatchBackupHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end checks against the real activity: hardware volume keys and opening backup files.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityIntegrationTest {

    // Avoid the runtime permission dialog stealing focus (and key events) on Android 13+.
    @get:Rule
    val notificationPermission: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            GrantPermissionRule.grant()
        }

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val viewModel: StopwatchViewModel
        get() = (context.applicationContext as WatchesApplication).stopwatchViewModel

    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)

    private fun clearWatches() = onMain {
        viewModel.uiState.value.stopwatches.forEach { viewModel.deleteStopwatch(it.id) }
        viewModel.dismissPendingImport()
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) {
                val state = viewModel.uiState.value
                "Condition not met within $timeoutMs ms (watches=${state.stopwatches.map { it.name }}, " +
                    "pendingImport=${state.pendingImportPayload?.stopwatches?.size})"
            }
            Thread.sleep(50)
        }
    }

    @Before
    fun setUp() {
        waitUntil { viewModel.isRestored.value }
        clearWatches()
    }

    @After
    fun tearDown() = clearWatches()

    @Test
    fun volumeUp_startsAllThenLaps_volumeDownStopsTarget() {
        onMain { viewModel.batchAddStopwatches(2, "Runner") }
        ActivityScenario.launch(MainActivity::class.java).use {
            instrumentation.waitForIdleSync()

            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP)
            waitUntil { viewModel.uiState.value.stopwatches.all { it.isRunning } }

            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_UP)
            waitUntil { viewModel.uiState.value.stopwatches.sumOf { it.currentRunLaps.size } == 1 }

            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_DOWN)
            waitUntil { viewModel.uiState.value.stopwatches.count { it.isRunning } == 1 }
        }
    }

    @Test
    fun volumeDown_shortPressWhenStopped_doesNotReset() {
        onMain {
            viewModel.addStopwatch("Runner")
            val id = viewModel.uiState.value.stopwatches.single().id
            viewModel.startStopwatch(id)
            viewModel.lapStopwatch(id)
            viewModel.pauseStopwatch(id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            instrumentation.waitForIdleSync()

            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_VOLUME_DOWN)
            instrumentation.waitForIdleSync()

            val watch = viewModel.uiState.value.stopwatches.single()
            assertFalse(watch.isRunning)
            assertEquals(1, watch.currentRunLaps.size)
        }
    }

    @Test
    fun openingBackupFile_importsWatches() {
        val backup = StopwatchBackupHelper.createBackupJson(
            listOf(
                StopwatchItem(name = "Imported A", pastelColorHex = 0xFF90CAF9),
                StopwatchItem(name = "Imported B", pastelColorHex = 0xFFA5D6A7)
            )
        )
        val file = File(context.cacheDir, "test.watches.json").apply { writeText(backup) }
        val uri = FileProvider.getUriForFile(context, "org.ear.watches.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setClass(context, MainActivity::class.java)
            .setDataAndType(uri, "application/json")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        // Plain startActivity: the activity consumes (clears) the intent data after importing,
        // which ActivityScenario's intent matching cannot follow.
        context.startActivity(intent)
        try {
            // With no existing watches the backup is imported without asking.
            waitUntil { viewModel.uiState.value.stopwatches.size == 2 }
            assertEquals(
                setOf("Imported A", "Imported B"),
                viewModel.uiState.value.stopwatches.map { it.name }.toSet()
            )
        } finally {
            onMain {
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .forEach { it.finish() }
            }
            file.delete()
        }
    }

    @Test
    fun importIntentFilter_matchesJsonOnly() {
        fun resolves(type: String): Boolean {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(android.net.Uri.parse("content://some.provider/file"), type)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setPackage(context.packageName)
            return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        }
        assertTrue(resolves("application/json"))
        assertFalse(resolves("application/pdf"))
        assertFalse(resolves("text/plain"))
    }
}
