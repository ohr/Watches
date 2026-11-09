package org.ear.watches.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.ear.watches.R
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.ui.dialogs.StopwatchDetailModal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The total and the live run duration in the detail dialog must follow the ticker. */
@RunWith(AndroidJUnit4::class)
class StopwatchDetailModalDisplayTest {

    @get:Rule
    val compose = createComposeRule()

    private val separator =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.subsecond_separator)

    @Test
    fun runningWatch_totalAndLiveRunFollowTicker_finishedRunStaysFixed() {
        val t0 = 1_000_000_000L
        // Run 1: 2 s, finished. Run 2: running since t0 + 10 s.
        val watch = StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9)
            .start(t0, 1L).pause(t0 + 2_000_000_000L)
            .start(t0 + 10_000_000_000L, 2L)
        val ticker = MutableStateFlow(t0 + 10_000_000_000L)

        compose.setContent {
            StopwatchDetailModal(watch = watch, settings = AppSettings(), tickerFlow = ticker, onDismiss = {})
        }

        ticker.value = t0 + 13_500_000_000L
        compose.waitForIdle()
        // Live run: 3.5 s; total: 2 s + 3.5 s = 5.5 s; finished run unchanged at 2.0 s.
        assertEquals(1, compose.onAllNodesWithText("00:03${separator}5").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("00:05${separator}5").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("00:02${separator}0").fetchSemanticsNodes().size)

        ticker.value = t0 + 20_000_000_000L
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithText("00:10${separator}0").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("00:12${separator}0").fetchSemanticsNodes().size)
        assertEquals(1, compose.onAllNodesWithText("00:02${separator}0").fetchSemanticsNodes().size)
    }
}
