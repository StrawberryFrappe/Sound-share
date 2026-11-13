package io.github.strawberryfrappe.soundshare.core

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

/**
 * The network half of the listening device.
 *
 * It says hello to the sender about once a second for as long as it runs, so
 * the stream resumes by itself after a Wi-Fi dropout or after the sender is
 * restarted, and hands every audio packet it gets to the [listener].
 */
class StreamClient(
    private val server: InetSocketAddress,
    private val listener: Listener,
) {
    interface Listener {
        /** Called on the network thread. The PCM is at [offset] in [buf] and is only valid during the call. */
        fun onAudio(sessionId: Int, seq: Long, buf: ByteArray, offset: Int, length: Int)

        /** The sender said it stopped sharing. */
        fun onSenderStopped()
    }

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
        // Network calls are not allowed on Android's main thread, where stop() is usually called.
        thread(name = "soundshare-bye", isDaemon = true) {
            send(s, Protocol.control(Protocol.TYPE_BYE))
            s.close()
        }
    }

    private fun receiveLoop(s: DatagramSocket) {
        val hello = Protocol.control(Protocol.TYPE_HELLO)
        val buf = ByteArray(Protocol.MAX_PACKET_SIZE)
        val incoming = DatagramPacket(buf, buf.size)
        var lastHello = 0L
        while (running) {
            val now = System.nanoTime()
            if (lastHello == 0L || now - lastHello >= Protocol.HELLO_INTERVAL_MS * 1_000_000) {
                send(s, hello)
                lastHello = now
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
                Protocol.TYPE_AUDIO -> if (incoming.length == Protocol.AUDIO_PACKET_SIZE) {
                    listener.onAudio(
                        Protocol.audioSessionId(buf),
                        Protocol.audioSeq(buf),
                        buf,
                        Protocol.AUDIO_HEADER_SIZE,
                        Protocol.PACKET_PCM_BYTES,
                    )
                }
                Protocol.TYPE_BYE -> listener.onSenderStopped()
            }
        }
    }

    private fun send(s: DatagramSocket, payload: ByteArray) {
        try {
            s.send(DatagramPacket(payload, payload.size, server))
        } catch (_: IOException) {
            // The network may be briefly away; the next hello tries again.
        }
    }

    private companion object {
        const val RECEIVE_TIMEOUT_MS = 250
        const val RECEIVE_BUFFER_BYTES = 256 * 1024
    }
}
