package org.ear.watches.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import org.ear.watches.R

/**
 * Timeout duration in milliseconds for button long-press detection (e.g. Full Reset or Session Naming).
 * Set to 300ms to trigger before the system drag-and-drop reorder long-press threshold (~500ms).
 */
private const val LONG_PRESS_TIMEOUT_MS = 300L

/**
 * Accessibility semantics for the raw pointer-input buttons below. Without them TalkBack and
 * Switch Access see no click action and cannot operate the timer controls.
 */
private fun Modifier.timerButtonSemantics(
    enabled: Boolean,
    onClick: () -> Unit,
    longClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = semantics(mergeDescendants = true) {
    role = Role.Button
    if (!enabled) disabled()
    onClick { if (enabled) onClick(); enabled }
    if (onLongClick != null) {
        onLongClick(label = longClickLabel) { if (enabled) onLongClick(); enabled }
    }
}

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
    val nameSessionLabel = stringResource(R.string.a11y_action_start_and_name_session)

    return this.timerButtonSemantics(
        enabled = enabled,
        onClick = { if (isRunning) onPause() else onStart() },
        // Mirrors the gesture: a long press on a stopped watch starts it and opens session naming.
        longClickLabel = nameSessionLabel,
        onLongClick = if (isRunning) null else ({ onStart(); onLongPressNameSession() })
    ).pointerInput(Unit) {
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
                        val releasedBeforeTimeout = withTimeoutOrNull(LONG_PRESS_TIMEOUT_MS) {
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
    return this.timerButtonSemantics(enabled = enabled, onClick = onPress).pointerInput(Unit) {
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
    val fullResetLabel = stringResource(R.string.a11y_action_full_reset)

    return this.timerButtonSemantics(
        enabled = enabled,
        onClick = onReset,
        longClickLabel = fullResetLabel,
        onLongClick = onFullReset
    ).pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press || event.changes.any { it.pressed }) {
                    event.changes.forEach { it.consume() }
                    var isReleased = false
                    val releasedBeforeTimeout = withTimeoutOrNull(LONG_PRESS_TIMEOUT_MS) {
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
