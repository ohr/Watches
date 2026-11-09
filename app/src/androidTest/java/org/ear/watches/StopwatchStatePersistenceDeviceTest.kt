package org.ear.watches

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.ear.watches.data.model.PersistedStopwatchState
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.repository.StopwatchStateRepository
import org.ear.watches.data.util.SystemTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Persistence against the real clocks and Settings.Global.BOOT_COUNT of a device. */
@RunWith(AndroidJUnit4::class)
class StopwatchStatePersistenceDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun bootCount_isAvailableOnDevice() {
        assertNotNull(StopwatchStateRepository.readBootCount(context))
    }

    @Test
    fun runningWatch_survivesSaveAndLoadOnSameBoot() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(context.cacheDir, "persistence-test-${System.nanoTime()}.preferences_pb")
        try {
            val repository = StopwatchStateRepository(
                PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
                bootCountProvider = { StopwatchStateRepository.readBootCount(context) }
            )
            val clock = SystemTimeProvider()
            val running = StopwatchItem(name = "A", pastelColorHex = 1L)
                .start(clock.currentTimeNanos(), clock.currentTimeMillis())

            repository.save(PersistedStopwatchState(stopwatches = listOf(running)))
            val restored = repository.load()!!.stopwatches.single()

            assertTrue(restored.isRunning)
            assertEquals(running.activeRun!!.startTimeNanos, restored.activeRun!!.startTimeNanos)
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
