package io.github.strawberryfrappe.soundshare.core

/**
 * Holds received audio packets for a short, chosen time before they are played.
 *
 * Packets come off Wi-Fi in bursts and sometimes out of order, while the
 * headphones need a perfectly steady stream. The network thread [push]es
 * packets in as they arrive; the audio thread [pull]s exactly one packet's
 * worth of sound each time the speaker wants more.
 *
 * The buffer aims to stay [targetPackets] deep. That depth is the listening
 * delay the user picks: deeper survives worse Wi-Fi and plays later.
 *
 * The two devices' audio clocks never agree exactly, so over an episode the
 * buffer would slowly fill up or run dry. To prevent that, [pull] plays one
 * sample frame fewer or one more per packet (a 0.4 % speed change, not
 * audible) whenever the average depth has wandered away from the target.
 */
class JitterBuffer(
    targetPackets: Int,
    private val packetBytes: Int = Protocol.PACKET_PCM_BYTES,
    private val frameBytes: Int = Protocol.BYTES_PER_FRAME,
    private val capacity: Int = 512,
) {
    class Stats(val bufferedPackets: Int, val lost: Long, val late: Long, val underruns: Long)

    private val data = ByteArray(capacity * packetBytes)
    private val slotSeq = LongArray(capacity) { NO_PACKET }

    private var target = targetPackets.coerceIn(1, capacity / 2)
    private var started = false
    private var buffering = true
    private var nextSeq = 0L
    private var highestSeq = -1L
    private var averageDepth = 0.0

    private var lost = 0L
    private var late = 0L
    private var underruns = 0L

    /** The largest number of bytes [pull] can write. */
    val maxPullBytes: Int get() = packetBytes + frameBytes

    var targetPackets: Int
        @Synchronized get() = target
        @Synchronized set(value) {
            val clamped = value.coerceIn(1, capacity / 2)
            if (clamped != target) {
                target = clamped
                // Jump straight to the new delay instead of drifting there over a minute.
                buffering = true
            }
        }

    @Synchronized
    fun push(seq: Long, src: ByteArray, offset: Int, length: Int) {
        if (length != packetBytes) return
        if (!started) {
            started = true
            buffering = true
            nextSeq = seq
            highestSeq = seq - 1
        }
        if (seq < nextSeq) {
            late++
            return
        }
        if (seq >= nextSeq + capacity) {
            // Far ahead of what we are playing: the stream jumped, start over from here.
            buffering = true
            nextSeq = seq
            highestSeq = seq - 1
        }
        val slot = (seq % capacity).toInt()
        System.arraycopy(src, offset, data, slot * packetBytes, packetBytes)
        slotSeq[slot] = seq
        if (seq > highestSeq) highestSeq = seq
    }

    /**
     * Writes the next stretch of audio into [out] and returns how many bytes
     * were written. It always writes something: silence while waiting for the
     * buffer to fill, and silence in place of a packet that never arrived.
     * [out] must hold at least [maxPullBytes].
     */
    @Synchronized
    fun pull(out: ByteArray): Int {
        var depth = if (started) (highestSeq - nextSeq + 1).toInt() else 0
        if (depth <= 0) {
            if (started && !buffering) {
                underruns++
                buffering = true
            }
            return silence(out)
        }
        if (buffering) {
            if (depth < target) return silence(out)
            buffering = false
            nextSeq = highestSeq - target + 1
            depth = target
            averageDepth = target.toDouble()
        } else if (depth > target + SKIP_SLACK_PACKETS) {
            // Too far behind to catch up gently; skip ahead.
            nextSeq = highestSeq - target + 1
            depth = target
            averageDepth = target.toDouble()
        }
        averageDepth += (depth - averageDepth) * SMOOTHING

        val slot = (nextSeq % capacity).toInt()
        if (slotSeq[slot] == nextSeq) {
            System.arraycopy(data, slot * packetBytes, out, 0, packetBytes)
        } else {
            out.fill(0, 0, packetBytes)
            lost++
        }
        nextSeq++

        return when {
            averageDepth > target + DRIFT_SLACK_PACKETS -> packetBytes - frameBytes
            averageDepth < target - DRIFT_SLACK_PACKETS -> {
                System.arraycopy(out, packetBytes - frameBytes, out, packetBytes, frameBytes)
                packetBytes + frameBytes
            }
            else -> packetBytes
        }
    }

    /** Forgets everything, for when the sender starts a new stream. */
    @Synchronized
    fun reset() {
        started = false
        buffering = true
        highestSeq = -1
        nextSeq = 0
        slotSeq.fill(NO_PACKET)
    }

    @Synchronized
    fun stats(): Stats {
        val depth = if (started) (highestSeq - nextSeq + 1).toInt().coerceAtLeast(0) else 0
        return Stats(depth, lost, late, underruns)
    }

    private fun silence(out: ByteArray): Int {
        out.fill(0, 0, packetBytes)
        return packetBytes
    }

    private companion object {
        const val NO_PACKET = -1L

        /** How far the average depth may sit from the target before playback speed is nudged. */
        const val DRIFT_SLACK_PACKETS = 1.5

        /** How far above the target the depth may get before the excess is thrown away. */
        const val SKIP_SLACK_PACKETS = 16

        /** Weight of the newest depth reading in the running average (about half a second). */
        const val SMOOTHING = 0.01
    }
}
