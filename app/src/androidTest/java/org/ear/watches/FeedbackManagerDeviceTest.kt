package org.ear.watches

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.ear.watches.util.FeedbackManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackManagerDeviceTest {

    @Test
    fun allCueSounds_initializeAndPlayWithoutError() {
        val feedback = FeedbackManager(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            FeedbackManager.Cue.entries.forEach { cue ->
                assertTrue("$cue sound not ready", feedback.isSoundReady(cue))
            }
            feedback.playStart(soundEnabled = true, hapticsEnabled = false)
            feedback.playLap(soundEnabled = true, hapticsEnabled = false)
            feedback.playLap(soundEnabled = true, hapticsEnabled = false) // rapid retrigger
            feedback.playStop(soundEnabled = true, hapticsEnabled = false)
            feedback.playReset(soundEnabled = true, hapticsEnabled = false)
        } finally {
            feedback.release()
        }
    }
}
