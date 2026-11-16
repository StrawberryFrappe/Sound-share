package io.github.strawberryfrappe.soundshare.core

import java.nio.ByteBuffer
import javax.crypto.SecretKey

/**
 * Wire format shared by the sending and the listening device.
 *
 * Everything travels in UDP datagrams that start with a 4-byte header:
 * `'S' 'S' <version> <type>`. Integers are big-endian; audio is 16-bit
 * little-endian PCM, 48 kHz stereo, 5 ms per packet, so one audio datagram
 * stays under a Wi-Fi MTU and is never fragmented.
 *
 * Only discovery is in the clear. Audio and the messages that control who
 * receives it are encrypted and authenticated with AES-GCM under a key derived
 * from the [PairingCode], so a device without the code can neither listen,
 * nor feed a listener sound, nor make the sender stream to anyone.
 *
 * Sender (owns [AUDIO_PORT]):
 * - [TYPE_BEACON], broadcast to [BEACON_PORT] every second and sent in reply to
 *   a [TYPE_PROBE]: key id, session nonce, port, device name. Cleartext.
 * - [TYPE_AUDIO]: session tag, sequence number, encrypted PCM.
 * - [TYPE_SENDER_BYE]: session tag, sequence number, authentication tag only.
 *
 * Listener:
 * - [TYPE_PROBE]: asks one address for its beacon. Padded to [PROBE_SIZE] so the
 *   reply is never bigger than the request.
 * - [TYPE_HELLO], about once a second while listening, and [TYPE_BYE]: listener
 *   id, counter, encrypted device name. The sender only streams to listeners
 *   whose latest hello it could decrypt and that are not replays.
 *
 * GCM nonces never repeat under one key: the key is fresh for every session,
 * the first nonce byte separates the kinds of packet, and the rest is a
 * per-sender sequence number or a random listener id with its own counter.
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

    const val VERSION = 2
    const val TYPE_AUDIO = 1
    const val TYPE_HELLO = 2
    const val TYPE_BYE = 3
    const val TYPE_BEACON = 4
    const val TYPE_PROBE = 5
    const val TYPE_SENDER_BYE = 6

    const val HEADER_SIZE = 4
    const val KEY_ID_SIZE = 8
    const val SESSION_NONCE_SIZE = 16
    const val LISTENER_ID_SIZE = 7

    /** Header, session tag, sequence number. */
    const val AUDIO_HEADER_SIZE = HEADER_SIZE + 8
    const val AUDIO_PACKET_SIZE = AUDIO_HEADER_SIZE + PACKET_PCM_BYTES + PacketCrypto.TAG_SIZE
    const val SENDER_BYE_SIZE = AUDIO_HEADER_SIZE + PacketCrypto.TAG_SIZE

    /** Header, listener id, counter. */
    const val LISTENER_HEADER_SIZE = HEADER_SIZE + LISTENER_ID_SIZE + 4

    const val BEACON_FIXED_SIZE = HEADER_SIZE + KEY_ID_SIZE + SESSION_NONCE_SIZE + 2
    const val MAX_NAME_BYTES = 64
    const val PROBE_SIZE = BEACON_FIXED_SIZE + MAX_NAME_BYTES
    const val MAX_PACKET_SIZE = 1400

    /** How often a listener says hello, and how long the sender keeps one that went quiet. */
    const val HELLO_INTERVAL_MS = 1000L
    const val CLIENT_TIMEOUT_MS = 5000L
    const val BEACON_INTERVAL_MS = 1000L
    const val MAX_LISTENERS = 8

    private const val MAGIC = 'S'.code.toByte()
    private const val NONCE_AUDIO: Byte = 1
    private const val NONCE_LISTENER: Byte = 2
    private const val NONCE_SENDER_BYE: Byte = 3

    /** Returns the packet type, or -1 if this is not one of our packets. */
    fun typeOf(buf: ByteArray, length: Int): Int {
        if (length < HEADER_SIZE) return -1
        if (buf[0] != MAGIC || buf[1] != MAGIC || buf[2].toInt() != VERSION) return -1
        return buf[3].toInt()
    }

    private fun putHeader(buf: ByteArray, type: Int) {
        buf[0] = MAGIC
        buf[1] = MAGIC
        buf[2] = VERSION.toByte()
        buf[3] = type.toByte()
    }

    /** The first four bytes of a session nonce, carried by every audio packet. */
    fun sessionTag(sessionNonce: ByteArray): Int = ByteBuffer.wrap(sessionNonce, 0, 4).int

    // --- Beacon and probe (cleartext) -------------------------------------------------------

    class Beacon(val keyId: ByteArray, val sessionNonce: ByteArray, val port: Int, val name: String)

    fun encodeBeacon(keyId: ByteArray, sessionNonce: ByteArray, port: Int, name: String): ByteArray {
        val nameBytes = truncateUtf8(name, MAX_NAME_BYTES)
        val packet = ByteArray(BEACON_FIXED_SIZE + nameBytes.size)
        putHeader(packet, TYPE_BEACON)
        ByteBuffer.wrap(packet, HEADER_SIZE, BEACON_FIXED_SIZE - HEADER_SIZE)
            .put(keyId, 0, KEY_ID_SIZE)
            .put(sessionNonce, 0, SESSION_NONCE_SIZE)
            .putShort(port.toShort())
        nameBytes.copyInto(packet, BEACON_FIXED_SIZE)
        return packet
    }

    fun decodeBeacon(buf: ByteArray, length: Int): Beacon? {
        if (typeOf(buf, length) != TYPE_BEACON || length < BEACON_FIXED_SIZE) return null
        val bb = ByteBuffer.wrap(buf, HEADER_SIZE, BEACON_FIXED_SIZE - HEADER_SIZE)
        val keyId = ByteArray(KEY_ID_SIZE).also { bb.get(it) }
        val sessionNonce = ByteArray(SESSION_NONCE_SIZE).also { bb.get(it) }
        val port = bb.short.toInt() and 0xFFFF
        val nameLength = (length - BEACON_FIXED_SIZE).coerceAtMost(MAX_NAME_BYTES)
        val name = String(buf, BEACON_FIXED_SIZE, nameLength, Charsets.UTF_8)
        return Beacon(keyId, sessionNonce, port, name)
    }

    fun probe(): ByteArray = ByteArray(PROBE_SIZE).also { putHeader(it, TYPE_PROBE) }

    // --- Audio and the sender's goodbye ------------------------------------------------------

    /**
     * Fills [packet] (at least [AUDIO_PACKET_SIZE] bytes) with one encrypted
     * audio packet holding [pcm], which must be [PACKET_PCM_BYTES] long.
     */
    fun sealAudio(packet: ByteArray, key: SecretKey, sessionTag: Int, seq: Int, pcm: ByteArray): Int {
        putSenderHeader(packet, TYPE_AUDIO, sessionTag, seq)
        return PacketCrypto.seal(key, senderNonce(NONCE_AUDIO, seq), packet, AUDIO_HEADER_SIZE, pcm, 0, PACKET_PCM_BYTES)
    }

    fun sealSenderBye(key: SecretKey, sessionTag: Int, seq: Int): ByteArray {
        val packet = ByteArray(SENDER_BYE_SIZE)
        putSenderHeader(packet, TYPE_SENDER_BYE, sessionTag, seq)
        PacketCrypto.seal(key, senderNonce(NONCE_SENDER_BYE, seq), packet, AUDIO_HEADER_SIZE, packet, 0, 0)
        return packet
    }

    fun packetSessionTag(packet: ByteArray): Int = ByteBuffer.wrap(packet, HEADER_SIZE, 4).int

    /** The packet counter, widened from an unsigned 32-bit value. */
    fun packetSeq(packet: ByteArray): Long =
        ByteBuffer.wrap(packet, HEADER_SIZE + 4, 4).int.toLong() and 0xFFFFFFFFL

    /** Decrypts an audio packet's PCM into [out]; returns false if it is forged or damaged. */
    fun openAudio(packet: ByteArray, length: Int, key: SecretKey, out: ByteArray): Boolean {
        if (length != AUDIO_PACKET_SIZE) return false
        val seq = packetSeq(packet).toInt()
        return PacketCrypto.open(key, senderNonce(NONCE_AUDIO, seq), packet, AUDIO_HEADER_SIZE, length, out, 0) ==
            PACKET_PCM_BYTES
    }

    fun verifySenderBye(packet: ByteArray, length: Int, key: SecretKey): Boolean {
        if (length != SENDER_BYE_SIZE) return false
        val seq = packetSeq(packet).toInt()
        return PacketCrypto.open(key, senderNonce(NONCE_SENDER_BYE, seq), packet, AUDIO_HEADER_SIZE, length, packet, 0) == 0
    }

    private fun putSenderHeader(packet: ByteArray, type: Int, sessionTag: Int, seq: Int) {
        putHeader(packet, type)
        ByteBuffer.wrap(packet, HEADER_SIZE, 8).putInt(sessionTag).putInt(seq)
    }

    private fun senderNonce(kind: Byte, seq: Int): ByteArray {
        val nonce = ByteArray(PacketCrypto.NONCE_SIZE)
        nonce[0] = kind
        ByteBuffer.wrap(nonce, 8, 4).putInt(seq)
        return nonce
    }

    // --- Listener hello and goodbye -----------------------------------------------------------

    class ListenerMessage(val type: Int, val listenerId: Long, val counter: Long, val name: String)

    /** A [TYPE_HELLO] or [TYPE_BYE] from the listener [listenerId], encrypted under the session [key]. */
    fun sealListenerMessage(type: Int, key: SecretKey, listenerId: ByteArray, counter: Long, name: String): ByteArray {
        val nameBytes = truncateUtf8(name, MAX_NAME_BYTES)
        val packet = ByteArray(LISTENER_HEADER_SIZE + nameBytes.size + PacketCrypto.TAG_SIZE)
        putHeader(packet, type)
        ByteBuffer.wrap(packet, HEADER_SIZE, LISTENER_ID_SIZE + 4)
            .put(listenerId, 0, LISTENER_ID_SIZE)
            .putInt(counter.toInt())
        PacketCrypto.seal(key, listenerNonce(packet), packet, LISTENER_HEADER_SIZE, nameBytes, 0, nameBytes.size)
        return packet
    }

    /** Checks and decrypts a listener's hello or goodbye; null if it was not made with [key]. */
    fun openListenerMessage(packet: ByteArray, length: Int, key: SecretKey): ListenerMessage? {
        val type = typeOf(packet, length)
        if (type != TYPE_HELLO && type != TYPE_BYE) return null
        if (length < LISTENER_HEADER_SIZE + PacketCrypto.TAG_SIZE ||
            length > LISTENER_HEADER_SIZE + MAX_NAME_BYTES + PacketCrypto.TAG_SIZE
        ) return null
        val name = ByteArray(MAX_NAME_BYTES)
        val nameLength = PacketCrypto.open(key, listenerNonce(packet), packet, LISTENER_HEADER_SIZE, length, name, 0)
        if (nameLength < 0) return null
        val bb = ByteBuffer.wrap(packet, HEADER_SIZE, LISTENER_ID_SIZE + 4)
        var listenerId = 0L
        repeat(LISTENER_ID_SIZE) { listenerId = (listenerId shl 8) or (bb.get().toLong() and 0xFF) }
        val counter = bb.int.toLong() and 0xFFFFFFFFL
        return ListenerMessage(type, listenerId, counter, String(name, 0, nameLength, Charsets.UTF_8))
    }

    private fun listenerNonce(packet: ByteArray): ByteArray {
        val nonce = ByteArray(PacketCrypto.NONCE_SIZE)
        nonce[0] = NONCE_LISTENER
        System.arraycopy(packet, HEADER_SIZE, nonce, 1, LISTENER_ID_SIZE + 4)
        return nonce
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
