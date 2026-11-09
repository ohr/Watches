package org.ear.watches.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.ui.components.StopwatchTileDefaults
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Guards WCAG AA contrast (4.5:1 for normal text) of the fixed tile colors. Tiles keep their
 * pastel background in light and dark theme, so one check covers both.
 */
class TileContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private val pastels = StopwatchItem.DEFAULT_PASTEL_COLORS.map { Color(it) }

    @Test
    fun textOnPastelTiles_meetsAA() {
        val foregrounds = mapOf(
            "content" to StopwatchTileDefaults.TileContentColor,
            "pace faster" to StopwatchTileDefaults.PaceFasterColor,
            "pace slower" to StopwatchTileDefaults.PaceSlowerColor,
            "status running" to StopwatchTileDefaults.StatusRunningColor,
            "status stopped" to StopwatchTileDefaults.StatusStoppedColor,
            "delete icon" to StopwatchTileDefaults.DeleteIconColor
        )
        foregrounds.forEach { (name, fg) ->
            pastels.forEach { bg ->
                val ratio = contrast(fg, bg)
                assertTrue(ratio >= 4.5, "$name on ${bg.value.toString(16)}: $ratio")
            }
        }
    }

    @Test
    fun whiteLabelsOnActionButtons_meetAA() {
        listOf(
            StopwatchTileDefaults.StartButtonColor,
            StopwatchTileDefaults.StopButtonColor,
            StopwatchTileDefaults.LapButtonColor,
            StopwatchTileDefaults.ResetButtonColor
        ).forEach { bg ->
            val ratio = contrast(Color.White, bg)
            assertTrue(ratio >= 4.5, "white on ${bg.value.toString(16)}: $ratio")
        }
    }
}
