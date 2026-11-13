package io.github.strawberryfrappe.soundshare.core

/**
 * Turns the hard edges around a gap in the sound into short fades.
 *
 * When a packet goes missing, the jitter buffer leaves 5 ms of silence. A
 * sudden drop to silence and back is heard as a click. This replaces the
 * first missing packet with the last one that arrived, fading out, and fades
 * the sound back in when it returns, so a lost packet sounds like a tiny dip.
 *
 * Works on 16-bit little-endian PCM. Call [process] on every pull, in order.
 */
class LossConcealer(
    private val packetBytes: Int = Protocol.PACKET_PCM_BYTES,
    private val frameBytes: Int = Protocol.BYTES_PER_FRAME,
) {
    private val lastAudio = ByteArray(packetBytes)
    private var previousWasAudio = false

    /** [out] holds [length] bytes from the jitter buffer; [hadAudio] says whether they are real sound. */
    fun process(out: ByteArray, length: Int, hadAudio: Boolean) {
        if (hadAudio) {
            if (!previousWasAudio) fade(out, length, fadeIn = true)
            System.arraycopy(out, 0, lastAudio, 0, minOf(length, packetBytes))
        } else if (previousWasAudio) {
            System.arraycopy(lastAudio, 0, out, 0, minOf(length, packetBytes))
            // A stretched pull is one frame longer than a packet; repeat the last frame for it.
            var at = packetBytes
            while (at + frameBytes <= length) {
                System.arraycopy(out, packetBytes - frameBytes, out, at, frameBytes)
                at += frameBytes
            }
            fade(out, length, fadeIn = false)
        }
        previousWasAudio = hadAudio
    }

    private fun fade(buf: ByteArray, length: Int, fadeIn: Boolean) {
        val frames = length / frameBytes
        for (frame in 0 until frames) {
            val step = frame.toFloat() / frames
            val gain = if (fadeIn) step else 1f - step
            var i = frame * frameBytes
            val end = i + frameBytes
            while (i < end) {
                val sample = (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
                val scaled = (sample * gain).toInt()
                buf[i] = scaled.toByte()
                buf[i + 1] = (scaled shr 8).toByte()
                i += 2
            }
        }
    }
}
