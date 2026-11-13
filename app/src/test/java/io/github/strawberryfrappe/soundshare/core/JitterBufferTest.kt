package io.github.strawberryfrappe.soundshare.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JitterBufferTest {
    private val packetBytes = Protocol.PACKET_PCM_BYTES
    private val frameBytes = Protocol.BYTES_PER_FRAME

    /** A packet whose every byte identifies it, so the test can tell which one was played. */
    private fun packet(seq: Long) = ByteArray(packetBytes) { (seq % 100 + 1).toByte() }

    private fun JitterBuffer.push(seq: Long) = push(seq, packet(seq), 0, packetBytes)

    private fun JitterBuffer.pullMarker(): Int {
        val out = ByteArray(maxPullBytes)
        pull(out)
        return out[0].toInt()
    }

    private fun markerOf(seq: Long) = (seq % 100 + 1).toInt()

    @Test
    fun staysSilentUntilTheTargetDepthIsReached() {
        val buffer = JitterBuffer(targetPackets = 4)
        assertEquals(0, buffer.pullMarker())
        for (seq in 0L..2L) buffer.push(seq)
        assertEquals(0, buffer.pullMarker())
        buffer.push(3)
        assertEquals(markerOf(0), buffer.pullMarker())
    }

    @Test
    fun playsPacketsInOrderEvenWhenTheyArriveShuffled() {
        val buffer = JitterBuffer(targetPackets = 4)
        for (seq in listOf(0L, 2L, 1L, 3L)) buffer.push(seq)
        val played = (0 until 4).map { buffer.pullMarker() }
        assertEquals((0L..3L).map(::markerOf), played)
    }

    @Test
    fun playsSilenceForAPacketThatNeverArrived() {
        val buffer = JitterBuffer(targetPackets = 4)
        for (seq in listOf(0L, 1L, 3L)) buffer.push(seq)
        val played = (0 until 4).map { buffer.pullMarker() }
        assertEquals(listOf(markerOf(0), markerOf(1), 0, markerOf(3)), played)
        assertEquals(1, buffer.stats().lost)
    }

    @Test
    fun ignoresAPacketThatArrivesAfterItsTurn() {
        val buffer = JitterBuffer(targetPackets = 2)
        for (seq in listOf(0L, 2L, 3L)) buffer.push(seq)
        buffer.pullMarker()
        buffer.pullMarker()
        buffer.pullMarker()
        buffer.push(1)
        assertEquals(1, buffer.stats().late)
    }

    @Test
    fun refillsToTheTargetAfterRunningDry() {
        val buffer = JitterBuffer(targetPackets = 3)
        for (seq in 0L..2L) buffer.push(seq)
        repeat(3) { buffer.pullMarker() }
        assertEquals(0, buffer.pullMarker())
        assertEquals(1, buffer.stats().underruns)

        buffer.push(3)
        assertEquals("still refilling", 0, buffer.pullMarker())
        buffer.push(4)
        buffer.push(5)
        assertEquals(markerOf(3), buffer.pullMarker())
    }

    @Test
    fun skipsAheadWhenFarTooMuchHasPiledUp() {
        val buffer = JitterBuffer(targetPackets = 4)
        for (seq in 0L..3L) buffer.push(seq)
        buffer.pullMarker()
        for (seq in 4L..60L) buffer.push(seq)
        assertEquals(markerOf(57), buffer.pullMarker())
        assertEquals(3, buffer.stats().bufferedPackets)
    }

    @Test
    fun changingTheTargetTakesEffectImmediately() {
        val buffer = JitterBuffer(targetPackets = 2)
        buffer.push(0)
        buffer.push(1)
        assertEquals(markerOf(0), buffer.pullMarker())
        for (seq in 2L..9L) buffer.push(seq)
        buffer.targetPackets = 6
        assertEquals(markerOf(4), buffer.pullMarker())
        assertEquals(5, buffer.stats().bufferedPackets)
    }

    @Test
    fun restartsCleanlyWhenTheStreamJumps() {
        val buffer = JitterBuffer(targetPackets = 2)
        buffer.push(0)
        buffer.push(1)
        buffer.pullMarker()
        buffer.push(100_000)
        buffer.push(100_001)
        assertEquals(markerOf(100_000), buffer.pullMarker())
    }

    @Test
    fun playsSlightlyFasterWhenTheSenderClockRunsAhead() {
        val result = simulateDrift(senderSpeed = 1.001)
        assertTrue("played audio should be shortened: $result", result.shortened > 0)
        assertEquals("no audio should be thrown away: $result", 0, result.lost)
        assertTrue("depth should stay near the target: $result", result.finalDepth in 8..13)
    }

    @Test
    fun playsSlightlySlowerWhenTheSenderClockRunsBehind() {
        val result = simulateDrift(senderSpeed = 0.999)
        assertTrue("played audio should be stretched: $result", result.lengthened > 0)
        assertEquals("the buffer should never run dry: $result", 0, result.underruns)
        assertTrue("depth should stay near the target: $result", result.finalDepth in 7..12)
    }

    private data class DriftResult(
        val shortened: Int,
        val lengthened: Int,
        val finalDepth: Int,
        val underruns: Long,
        val lost: Long,
    )

    /**
     * Plays two minutes of audio while the sender's clock runs [senderSpeed]
     * times as fast as the listener's. Real devices differ by a few
     * thousandths of a percent; 0.1 % is far harsher, and uncorrected it
     * would move the buffer by 24 packets, more than it holds here.
     */
    private fun simulateDrift(senderSpeed: Double): DriftResult {
        val framesPerPacket = Protocol.FRAMES_PER_PACKET
        val buffer = JitterBuffer(targetPackets = 10)
        val out = ByteArray(buffer.maxPullBytes)
        var sent = 0L
        repeat(10) { buffer.push(sent++) }
        var playedFrames = 0L
        var shortened = 0
        var lengthened = 0
        while (playedFrames < 120L * Protocol.SAMPLE_RATE) {
            val senderFrames = (playedFrames * senderSpeed).toLong()
            while ((sent - 10) * framesPerPacket < senderFrames) buffer.push(sent++)
            val bytes = buffer.pull(out)
            when (bytes) {
                packetBytes - frameBytes -> shortened++
                packetBytes + frameBytes -> lengthened++
            }
            playedFrames += bytes / frameBytes
        }
        val stats = buffer.stats()
        return DriftResult(shortened, lengthened, stats.bufferedPackets, stats.underruns, stats.lost)
    }
}
