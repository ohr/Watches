package org.ear.watches.data.util

import android.os.SystemClock

/**
 * Abstraction for retrieving current monotonic and wall-clock time.
 * Used primarily to allow injecting mock time providers during testing,
 * while the default implementation uses the system clock.
 */
interface TimeProvider {
    fun currentTimeNanos(): Long
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeNanos(): Long = SystemClock.elapsedRealtimeNanos()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
