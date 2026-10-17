package org.ear.watches.data.util

/**
 * Abstraction for retrieving the current time. 
 * Used primarily to allow injecting mock time providers during testing, 
 * while the default implementation uses the system clock.
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
