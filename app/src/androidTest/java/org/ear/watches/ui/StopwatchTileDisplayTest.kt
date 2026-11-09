package org.ear.watches.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.ear.watches.R
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.ui.components.StopwatchTile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The time text must follow the ticker while running (it is the only part that reads it). */
@RunWith(AndroidJUnit4::class)
class StopwatchTileDisplayTest {

    @get:Rule
    val compose = createComposeRule()

    private val separator =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.subsecond_separator)

    private fun setTile(watch: StopwatchItem, ticker: MutableStateFlow<Long>) {
        compose.setContent {
            StopwatchTile(
                watch = watch,
                timePrecision = AppPrecision.TENTHS,
                tickerFlow = ticker,
                onStart = {}, onPause = {}, onLap = {}, onReset = {},
                onDelete = {}, onEdit = {}, onOpenDetails = {}, onSelectFocus = {}
            )
        }
    }

    @Test
    fun runningWatch_displayFollowsTicker() {
        val start = 1_000_000_000L
        val ticker = MutableStateFlow(start)
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9).start(start, 1L), ticker)

        ticker.value = start + 1_200_000_000L
        compose.onNodeWithText("00:01${separator}2").assertExists()

        ticker.value = start + 61_500_000_000L
        compose.onNodeWithText("01:01${separator}5").assertExists()
    }

    @Test
    fun stoppedWatch_ignoresTicker() {
        val ticker = MutableStateFlow(10_000_000_000L)
        val stopped = StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9)
            .start(1_000_000_000L, 1L)
            .pause(4_000_000_000L)
        setTile(stopped, ticker)

        ticker.value = 99_000_000_000L
        compose.onNodeWithText("00:03${separator}0").assertExists()
    }
}
