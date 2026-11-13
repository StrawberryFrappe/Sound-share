package io.github.strawberryfrappe.soundshare.core

import java.nio.ByteBuffer

/**
 * Wire format shared by the sending and the listening device.
 *
 * Everything travels in UDP datagrams that start with a 4-byte header:
 * `'S' 'S' <version> <type>`. Integers are big-endian; audio is raw
 * 16-bit little-endian PCM, 48 kHz stereo, 5 ms per packet, so one audio
 * datagram stays well under a Wi-Fi MTU and is never fragmented.
 *
 * - The sender owns [AUDIO_PORT]. A listener sends [TYPE_HELLO] to it about
 *   once a second and gets the audio stream back on the same socket.
 * - The sender announces itself with [TYPE_BEACON] broadcasts to [BEACON_PORT].
 * - [TYPE_BYE] tells the other side that this end is going away.
 */
object Protocol {
    const val AUDIO_PORT = 47800
    const val BEACON_PORT = 47801

    const val SAMPLE_RATE = 48000
    const val CHANNELS = 2
    const val BYTES_PER_FRAME = CHANNELS * 2
    const val PACKET_MS = 5
    const val FRAMES_PER_PACKET = SAMPLE_RATE * PACKET_MS / 1000
    const val PACKET_PCM_BYTES = FRAMES_PER_PACKET * BYTES_PER_FRAME

    const val VERSION = 1
    const val TYPE_AUDIO = 1
    const val TYPE_HELLO = 2
    const val TYPE_BYE = 3
    const val TYPE_BEACON = 4

    const val HEADER_SIZE = 4
    const val AUDIO_HEADER_SIZE = HEADER_SIZE + 8
    const val AUDIO_PACKET_SIZE = AUDIO_HEADER_SIZE + PACKET_PCM_BYTES
    const val MAX_NAME_BYTES = 64
    const val MAX_PACKET_SIZE = 1400

    /** How often a listener says hello, and how long the sender keeps one that went quiet. */
    const val HELLO_INTERVAL_MS = 1000L
    const val CLIENT_TIMEOUT_MS = 5000L
    const val BEACON_INTERVAL_MS = 1000L

    private const val MAGIC_0 = 'S'.code.toByte()
    private const val MAGIC_1 = 'S'.code.toByte()

    /** Returns the packet type, or -1 if this is not one of our packets. */
    fun typeOf(buf: ByteArray, length: Int): Int {
        if (length < HEADER_SIZE) return -1
        if (buf[0] != MAGIC_0 || buf[1] != MAGIC_1 || buf[2].toInt() != VERSION) return -1
        return buf[3].toInt()
    }

    private fun putHeader(buf: ByteArray, type: Int) {
        buf[0] = MAGIC_0
        buf[1] = MAGIC_1
        buf[2] = VERSION.toByte()
        buf[3] = type.toByte()
    }

    /** A packet that is only a header: [TYPE_HELLO] or [TYPE_BYE]. */
    fun control(type: Int): ByteArray = ByteArray(HEADER_SIZE).also { putHeader(it, type) }

    /** Writes the audio header into [packet]; the PCM goes right after it at [AUDIO_HEADER_SIZE]. */
    fun putAudioHeader(packet: ByteArray, sessionId: Int, seq: Int) {
        putHeader(packet, TYPE_AUDIO)
        ByteBuffer.wrap(packet, HEADER_SIZE, 8).putInt(sessionId).putInt(seq)
    }

    fun audioSessionId(packet: ByteArray): Int = ByteBuffer.wrap(packet, HEADER_SIZE, 4).int

    /** The packet counter, widened from an unsigned 32-bit value. */
    fun audioSeq(packet: ByteArray): Long =
        ByteBuffer.wrap(packet, HEADER_SIZE + 4, 4).int.toLong() and 0xFFFFFFFFL

    class Beacon(val sessionId: Int, val port: Int, val name: String)

    fun encodeBeacon(sessionId: Int, port: Int, name: String): ByteArray {
        val nameBytes = truncateUtf8(name, MAX_NAME_BYTES)
        val packet = ByteArray(HEADER_SIZE + 6 + nameBytes.size)
        putHeader(packet, TYPE_BEACON)
        ByteBuffer.wrap(packet, HEADER_SIZE, 6).putInt(sessionId).putShort(port.toShort())
        nameBytes.copyInto(packet, HEADER_SIZE + 6)
        return packet
    }

    fun decodeBeacon(buf: ByteArray, length: Int): Beacon? {
        if (typeOf(buf, length) != TYPE_BEACON || length < HEADER_SIZE + 6) return null
        val bb = ByteBuffer.wrap(buf, HEADER_SIZE, 6)
        val sessionId = bb.int
        val port = bb.short.toInt() and 0xFFFF
        val nameLength = (length - HEADER_SIZE - 6).coerceAtMost(MAX_NAME_BYTES)
        val name = String(buf, HEADER_SIZE + 6, nameLength, Charsets.UTF_8)
        return Beacon(sessionId, port, name)
    }

    /** Cuts [text] to at most [maxBytes] of UTF-8 without splitting a character. */
    private fun truncateUtf8(text: String, maxBytes: Int): ByteArray {
        var end = text.length
        while (true) {
            val bytes = text.substring(0, end).toByteArray(Charsets.UTF_8)
            if (bytes.size <= maxBytes) return bytes
            end--
            if (end > 0 && Character.isLowSurrogate(text[end])) end--
        }
    }
}
