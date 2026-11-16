package io.github.strawberryfrappe.soundshare.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceWatchTest {
    private val second = 1_000_000_000L
    private val silence = ByteArray(Protocol.PACKET_PCM_BYTES)
    private val sound = ByteArray(Protocol.PACKET_PCM_BYTES).also { it[500] = 3 }

    /** Feeds packets 5 ms apart from [start] for [seconds], returns the watch's state. */
    private fun SilenceWatch.feed(pcm: ByteArray, playing: Boolean, start: Long, seconds: Double): Boolean {
        val packets = (seconds * 200).toInt()
        for (i in 0 until packets) onPacket(pcm, pcm.size, playing, start + i * 5_000_000L)
        return blocked
    }

    @Test
    fun silenceWhileSomethingPlaysIsReportedAfterEightSeconds() {
        val watch = SilenceWatch()
        assertFalse(watch.feed(silence, playing = true, start = 0, seconds = 7.9))
        assertTrue(watch.feed(silence, playing = true, start = (7.9 * second).toLong(), seconds = 0.2))
    }

    @Test
    fun silenceWithNothingPlayingIsNotReported() {
        val watch = SilenceWatch()
        assertFalse("a paused video is not a blocked app", watch.feed(silence, playing = false, start = 0, seconds = 30.0))
    }

    @Test
    fun anySoundClearsTheWarningAtOnce() {
        val watch = SilenceWatch()
        assertTrue(watch.feed(silence, playing = true, start = 0, seconds = 9.0))
        assertTrue(watch.onPacket(sound, sound.size, true, 9 * second))
        assertFalse(watch.blocked)
    }

    @Test
    fun shortQuietMomentsDoNotAddUp() {
        val watch = SilenceWatch()
        var t = 0L
        repeat(10) {
            assertFalse(watch.feed(silence, playing = true, start = t, seconds = 6.0))
            t += 6 * second
            watch.onPacket(sound, sound.size, true, t)
            t += 5_000_000L
        }
    }

    @Test
    fun pausingResetsTheCount() {
        val watch = SilenceWatch()
        watch.feed(silence, playing = true, start = 0, seconds = 6.0)
        watch.feed(silence, playing = false, start = 6 * second, seconds = 1.0)
        assertFalse(watch.feed(silence, playing = true, start = 7 * second, seconds = 6.0))
    }

    @Test
    fun theReturnValueSaysWhenTheStateChanged() {
        val watch = SilenceWatch(thresholdNanos = 0)
        assertTrue(watch.onPacket(silence, silence.size, true, 0))
        assertFalse(watch.onPacket(silence, silence.size, true, 1))
        assertTrue(watch.onPacket(silence, silence.size, false, 2))
    }
}
