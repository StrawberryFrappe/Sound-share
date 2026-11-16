package io.github.strawberryfrappe.soundshare.core

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.SecretKey
import kotlin.concurrent.thread

/**
 * The network half of the listening device.
 *
 * It asks the sender for its current session, proves it holds the pairing
 * code by saying an encrypted hello about once a second, and hands every
 * audio packet that decrypts to the [listener]. It keeps going by itself
 * through Wi-Fi dropouts and through the sender restarting its stream.
 */
class StreamClient(
    private val server: InetSocketAddress,
    private val code: PairingCode,
    private val deviceName: String,
    private val listener: Listener,
    random: SecureRandom = SecureRandom(),
) {
    interface Listener {
        /** Called on the network thread. The PCM is at [offset] in [buf] and is only valid during the call. */
        fun onAudio(sessionTag: Int, seq: Long, buf: ByteArray, offset: Int, length: Int)

        /** The sender said it stopped sharing. */
        fun onSenderStopped()

        /** The device at this address has a different pairing code: it made a new one, or it is another device. */
        fun onWrongCode()
    }

    private val listenerId = ByteArray(Protocol.LISTENER_ID_SIZE).also(random::nextBytes)
    // Shared by the receive thread and the goodbye thread; a repeated value would reuse a nonce.
    private val counter = AtomicLong()

    // Only touched by the receive thread, except for the final goodbye.
    @Volatile
    private var key: SecretKey? = null
    private var sessionTag = 0
    private var lastAudioNanos = 0L
    private var sessionStartNanos = 0L
    private var reportedWrongCode = false

    private var socket: DatagramSocket? = null

    @Volatile
    private var running = false

    @Throws(IOException::class)
    fun start() {
        val s = DatagramSocket()
        s.soTimeout = RECEIVE_TIMEOUT_MS
        // Room for about a second of audio, so a burst after a Wi-Fi stall is not thrown away.
        s.receiveBufferSize = RECEIVE_BUFFER_BYTES
        socket = s
        running = true
        thread(name = "soundshare-receive", isDaemon = true, priority = Thread.MAX_PRIORITY) { receiveLoop(s) }
    }

    fun stop() {
        if (!running) return
        running = false
        val s = socket ?: return
        val sessionKey = key
        // Network calls are not allowed on Android's main thread, where stop() is usually called.
        thread(name = "soundshare-bye", isDaemon = true) {
            if (sessionKey != null) {
                send(s, Protocol.sealListenerMessage(Protocol.TYPE_BYE, sessionKey, listenerId, counter.incrementAndGet(), deviceName))
            }
            s.close()
        }
    }

    private fun receiveLoop(s: DatagramSocket) {
        val buf = ByteArray(Protocol.MAX_PACKET_SIZE)
        val pcm = ByteArray(Protocol.PACKET_PCM_BYTES)
        val incoming = DatagramPacket(buf, buf.size)
        var lastSent = 0L
        while (running) {
            val now = System.nanoTime()
            val sessionKey = key
            if (sessionKey != null && now - maxOf(lastAudioNanos, sessionStartNanos) > SESSION_SILENCE_NANOS) {
                // Hellos are not getting through, or the sender started a new session: ask again.
                key = null
            }
            if (lastSent == 0L || now - lastSent >= Protocol.HELLO_INTERVAL_MS * 1_000_000) {
                sayHelloOrProbe(s)
                lastSent = now
            }
            try {
                incoming.length = buf.size
                s.receive(incoming)
            } catch (_: SocketTimeoutException) {
                continue
            } catch (_: SocketException) {
                break
            } catch (_: IOException) {
                continue
            }
            if (incoming.address != server.address) continue
            when (Protocol.typeOf(buf, incoming.length)) {
                Protocol.TYPE_BEACON -> {
                    val beacon = Protocol.decodeBeacon(buf, incoming.length) ?: continue
                    if (onBeacon(beacon)) {
                        sayHelloOrProbe(s)
                        lastSent = System.nanoTime()
                    }
                }
                Protocol.TYPE_AUDIO -> {
                    val currentKey = key ?: continue
                    if (Protocol.packetSessionTag(buf) != sessionTag) {
                        key = null
                        continue
                    }
                    if (!Protocol.openAudio(buf, incoming.length, currentKey, pcm)) continue
                    lastAudioNanos = System.nanoTime()
                    listener.onAudio(sessionTag, Protocol.packetSeq(buf), pcm, 0, pcm.size)
                }
                Protocol.TYPE_SENDER_BYE -> {
                    val currentKey = key ?: continue
                    if (Protocol.packetSessionTag(buf) == sessionTag &&
                        Protocol.verifySenderBye(buf, incoming.length, currentKey)
                    ) {
                        key = null
                        listener.onSenderStopped()
                    }
                }
            }
        }
    }

    /** Takes up the session a beacon announces. Returns true if it is a new one. */
    private fun onBeacon(beacon: Protocol.Beacon): Boolean {
        if (!beacon.keyId.contentEquals(code.keyId)) {
            if (!reportedWrongCode) {
                reportedWrongCode = true
                listener.onWrongCode()
            }
            return false
        }
        reportedWrongCode = false
        val tag = Protocol.sessionTag(beacon.sessionNonce)
        if (key != null && tag == sessionTag) return false
        key = code.sessionKey(beacon.sessionNonce)
        sessionTag = tag
        sessionStartNanos = System.nanoTime()
        return true
    }

    private fun sayHelloOrProbe(s: DatagramSocket) {
        val sessionKey = key
        if (sessionKey == null) {
            send(s, Protocol.probe())
        } else {
            send(s, Protocol.sealListenerMessage(Protocol.TYPE_HELLO, sessionKey, listenerId, counter.incrementAndGet(), deviceName))
        }
    }

    private fun send(s: DatagramSocket, payload: ByteArray) {
        try {
            s.send(DatagramPacket(payload, payload.size, server))
        } catch (_: IOException) {
            // The network may be briefly away; the next round tries again.
        }
    }

    private companion object {
        const val RECEIVE_TIMEOUT_MS = 250
        const val RECEIVE_BUFFER_BYTES = 256 * 1024
        const val SESSION_SILENCE_NANOS = 3_000_000_000L
    }
}
