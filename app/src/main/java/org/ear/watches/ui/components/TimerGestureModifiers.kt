package org.ear.watches.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Custom pointer-input gesture modifier for instant response start/pause press
 * with long-press detection for session naming.
 */
@Composable
fun Modifier.onStartInstantPressWithLongPressNaming(
    enabled: Boolean = true,
    isRunning: Boolean = false,
    onStart: () -> Unit,
    onPause: () -> Unit = {},
    onLongPressNameSession: () -> Unit,
    onLongPressHaptic: () -> Unit = {}
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentIsRunning by rememberUpdatedState(isRunning)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnLongPressNameSession by rememberUpdatedState(onLongPressNameSession)
    val currentOnLongPressHaptic by rememberUpdatedState(onLongPressHaptic)

    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press || event.changes.any { it.pressed }) {
                    event.changes.forEach { it.consume() }
                    val wasRunning = currentIsRunning
                    if (wasRunning) {
                        currentOnPause()
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    } else {
                        currentOnStart()
                        var isReleased = false
                        val releasedBeforeTimeout = withTimeoutOrNull(500L) {
                            while (true) {
                                val nextEvent = awaitPointerEvent()
                                nextEvent.changes.forEach { it.consume() }
                                if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                    isReleased = true
                                    break
                                }
                            }
                            true
                        }

                        if (releasedBeforeTimeout == null && !isReleased) {
                            currentOnLongPressHaptic()
                            currentOnLongPressNameSession()
                            while (true) {
                                val nextEvent = awaitPointerEvent()
                                nextEvent.changes.forEach { it.consume() }
                                if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom pointer-input gesture modifier for instant response press (e.g. Lap button).
 */
@Composable
fun Modifier.onInstantPress(
    enabled: Boolean = true,
    onPress: () -> Unit
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnPress by rememberUpdatedState(onPress)
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press) {
                    event.changes.forEach { it.consume() }
                    currentOnPress()
                }
            }
        }
    }
}

/**
 * Custom pointer-input gesture modifier for instant response reset press
 * with long-press detection for full reset.
 */
@Composable
fun Modifier.onResetInstantPress(
    enabled: Boolean = true,
    onReset: () -> Unit,
    onFullReset: () -> Unit,
    onLongPressHaptic: () -> Unit = {}
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnFullReset by rememberUpdatedState(onFullReset)
    val currentOnLongPressHaptic by rememberUpdatedState(onLongPressHaptic)

    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press || event.changes.any { it.pressed }) {
                    event.changes.forEach { it.consume() }
                    var isReleased = false
                    val releasedBeforeTimeout = withTimeoutOrNull(500L) {
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                isReleased = true
                                break
                            }
                        }
                        true
                    }

                    if (releasedBeforeTimeout != null) {
                        if (isReleased) {
                            currentOnReset()
                        }
                    } else {
                        currentOnLongPressHaptic()
                        currentOnFullReset()
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}
