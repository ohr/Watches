package org.ear.watches.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.ear.watches.R
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.ui.components.StopwatchTile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the touch-down behavior of the tile controls (timing must start on press, not on
 * release) and that the same controls are operable through accessibility actions.
 */
@RunWith(AndroidJUnit4::class)
class StopwatchTileGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setTile(watch: StopwatchItem) {
        compose.setContent {
            StopwatchTile(
                watch = watch,
                timePrecision = AppPrecision.TENTHS,
                tickerFlow = MutableStateFlow(0L),
                onStart = { events += "start" },
                onPause = { events += "pause" },
                onLap = { events += "lap" },
                onReset = { events += "reset" },
                onFullReset = { events += "fullReset" },
                onNameSession = { events += "name" },
                onDelete = {},
                onEdit = {},
                onOpenDetails = {},
                onSelectFocus = {}
            )
        }
    }

    private fun button(labelRes: Int): SemanticsNodeInteraction =
        compose.onNode(hasClickAction() and hasText(context.getString(labelRes), ignoreCase = true))

    @Before
    fun clear() = events.clear()

    @Test
    fun start_firesOnTouchDown_beforeRelease() {
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9))

        button(R.string.btn_start).performTouchInput { down(center) }
        compose.waitForIdle()

        assertEquals(listOf("start"), events)
    }

    @Test
    fun lap_firesOnTouchDown() {
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9).start(1_000L, 1L))

        button(R.string.btn_lap).performTouchInput { down(center) }
        compose.waitForIdle()

        assertEquals(listOf("lap"), events)
    }

    @Test
    fun reset_shortPressResets_longPressFullyResets() {
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9))

        button(R.string.btn_reset).performTouchInput { down(center); up() }
        compose.waitForIdle()
        assertEquals(listOf("reset"), events)

        events.clear()
        button(R.string.btn_reset).performTouchInput { down(center); advanceEventTime(600); up() }
        compose.waitForIdle()
        assertEquals(listOf("fullReset"), events)
    }

    @Test
    fun controls_areOperableViaAccessibilityActions() {
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9))

        button(R.string.btn_start).performSemanticsAction(SemanticsActions.OnClick)
        button(R.string.btn_start).performSemanticsAction(SemanticsActions.OnLongClick)
        button(R.string.btn_reset).performSemanticsAction(SemanticsActions.OnClick)
        button(R.string.btn_reset).performSemanticsAction(SemanticsActions.OnLongClick)
        compose.waitForIdle()

        assertEquals(listOf("start", "start", "name", "reset", "fullReset"), events)
    }

    @Test
    fun disabledLap_isExposedAsDisabled() {
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9))

        compose.onNode(
            hasText(context.getString(R.string.btn_lap), ignoreCase = true) and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button)
        ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Disabled))
    }

    @Test
    fun everyClickableNode_hasALabel() {
        setTile(StopwatchItem(name = "A", pastelColorHex = 0xFF90CAF9).start(1_000L, 1L))

        val nodes = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty())
        nodes.forEach { node ->
            val config = node.config
            val labelled = config.contains(SemanticsProperties.ContentDescription) ||
                config.contains(SemanticsProperties.Text)
            assertTrue("Unlabelled clickable node: $config", labelled)
        }
    }
}
