package org.ear.watches.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RunSessionTest {

    @Test
    fun domainHelpers_defaultUnstartedSession() {
        val run = RunSession(runIndex = 1)

        assertTrue(run.isUnstarted)
        assertFalse(run.isStarted)
        assertFalse(run.isRunning)
        assertFalse(run.isFinished)
        assertEquals(0L, run.accumulatedMillis)
    }

    @Test
    fun domainHelpers_activeRunningSession() {
        val run = RunSession(
            runIndex = 1,
            startTimeNanos = 1_000_000_000L,
            startTimeMillis = 1_000L,
        )

        assertFalse(run.isUnstarted)
        assertTrue(run.isStarted)
        assertTrue(run.isRunning)
        assertFalse(run.isFinished)
    }

    @Test
    fun domainHelpers_finishedSession() {
        val run = RunSession(
            runIndex = 1,
            startTimeNanos = 1_000_000_000L,
            endTimeNanos = 6_000_000_000L,
            accumulatedNanos = 5_000_000_000L,
            startTimeMillis = 1_000L
        )

        assertFalse(run.isUnstarted)
        assertTrue(run.isStarted)
        assertFalse(run.isRunning)
        assertTrue(run.isFinished)
        assertEquals(5000L, run.accumulatedMillis)
    }
}
