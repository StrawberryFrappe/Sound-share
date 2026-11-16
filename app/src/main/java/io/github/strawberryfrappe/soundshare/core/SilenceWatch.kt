package io.github.strawberryfrappe.soundshare.core

/**
 * Notices when sharing captures nothing but digital silence while something on
 * the device is playing: the sign of an app that does not allow its sound to be
 * captured (Netflix, for example). Real soundtracks are rarely exact zeros
 * for many seconds, while a blocked app gives exactly that. A muted player or
 * a long silent stretch can still trigger it, so the warning says "may".
 *
 * Feed it every captured packet, in order, from one thread.
 */
class SilenceWatch(private val thresholdNanos: Long = DEFAULT_THRESHOLD_NANOS) {
    private var silentSince = NOT_SILENT

    /** Whether sharing is currently getting only silence from something that is playing. */
    var blocked = false
        private set

    /**
     * Takes one packet of 16-bit PCM. [mediaPlaying] says whether some app is
     * playing media right now; [nowNanos] is a monotonic clock reading.
     * Returns true when [blocked] changed.
     */
    fun onPacket(pcm: ByteArray, length: Int, mediaPlaying: Boolean, nowNanos: Long): Boolean {
        val wasBlocked = blocked
        if (!mediaPlaying || !isDigitalSilence(pcm, length)) {
            silentSince = NOT_SILENT
            blocked = false
        } else {
            if (silentSince == NOT_SILENT) silentSince = nowNanos
            blocked = nowNanos - silentSince >= thresholdNanos
        }
        return blocked != wasBlocked
    }

    private fun isDigitalSilence(pcm: ByteArray, length: Int): Boolean {
        for (i in 0 until length) if (pcm[i].toInt() != 0) return false
        return true
    }

    private companion object {
        const val NOT_SILENT = Long.MIN_VALUE
        const val DEFAULT_THRESHOLD_NANOS = 8_000_000_000L
    }
}
