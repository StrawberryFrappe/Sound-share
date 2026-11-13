package io.github.strawberryfrappe.soundshare.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LossConcealerTest {
    private val packetBytes = Protocol.PACKET_PCM_BYTES
    private val frameBytes = Protocol.BYTES_PER_FRAME
    private val frames = Protocol.FRAMES_PER_PACKET

    /** A packet where every sample has the same value. */
    private fun steady(value: Int) = ByteArray(packetBytes).also { buf ->
        for (i in buf.indices step 2) {
            buf[i] = value.toByte()
            buf[i + 1] = (value shr 8).toByte()
        }
    }

    private fun sample(buf: ByteArray, frame: Int, channel: Int = 0): Int {
        val i = frame * frameBytes + channel * 2
        return (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
    }

    @Test
    fun soundFadesInWhenItStarts() {
        val concealer = LossConcealer()
        val out = steady(10_000)
        concealer.process(out, packetBytes, hadAudio = true)
        assertEquals(0, sample(out, 0))
        assertTrue(sample(out, frames / 2) in 4_900..5_100)
        assertTrue(sample(out, frames - 1) > 9_900)
    }

    @Test
    fun soundInTheMiddleOfAStreamIsLeftAlone() {
        val concealer = LossConcealer()
        concealer.process(steady(10_000), packetBytes, hadAudio = true)
        val out = steady(-12_345)
        concealer.process(out, packetBytes, hadAudio = true)
        assertArrayEquals(steady(-12_345), out)
    }

    @Test
    fun aMissingPacketBecomesTheLastOneFadingOut() {
        val concealer = LossConcealer()
        concealer.process(steady(10_000), packetBytes, hadAudio = true)
        concealer.process(steady(-8_000), packetBytes, hadAudio = true)
        val out = ByteArray(packetBytes)
        concealer.process(out, packetBytes, hadAudio = false)
        assertEquals(-8_000, sample(out, 0))
        assertEquals(-8_000, sample(out, 0, channel = 1))
        assertTrue(abs(sample(out, frames - 1)) < 100)

        val next = ByteArray(packetBytes)
        concealer.process(next, packetBytes, hadAudio = false)
        assertArrayEquals("a second gap in a row stays silent", ByteArray(packetBytes), next)
    }

    @Test
    fun aStretchedGapIsFilledToTheEnd() {
        val concealer = LossConcealer()
        concealer.process(steady(10_000), packetBytes, hadAudio = true)
        concealer.process(steady(10_000), packetBytes, hadAudio = true)
        val out = ByteArray(packetBytes + frameBytes)
        concealer.process(out, packetBytes + frameBytes, hadAudio = false)
        assertEquals(10_000, sample(out, 0))
        assertTrue(sample(out, frames) in 1..100)
    }
}
