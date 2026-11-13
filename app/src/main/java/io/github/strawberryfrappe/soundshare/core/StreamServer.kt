package io.github.strawberryfrappe.soundshare.core

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketAddress
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * The network half of the sending device.
 *
 * It keeps a list of listeners (anyone who said hello in the last few
 * seconds), sends every audio packet to each of them, and broadcasts a beacon
 * on every local network so listeners can find this device without typing an
 * address.
 */
class StreamServer(
    private val deviceName: String,
    private val port: Int = Protocol.AUDIO_PORT,
    private val beaconPort: Int = Protocol.BEACON_PORT,
) {
    val sessionId: Int = Random.nextInt()

    private var socket: DatagramSocket? = null
    private val clients = ConcurrentHashMap<SocketAddress, Long>()
    private val packet = ByteArray(Protocol.AUDIO_PACKET_SIZE)
    private var seq = 0

    @Volatile
    private var running = false

    val clientCount: Int get() = clients.size

    /** The port actually bound, which differs from the requested one when that was 0. */
    val localPort: Int get() = socket?.localPort ?: -1

    @Throws(IOException::class)
    fun start() {
        val s = DatagramSocket(null)
        s.reuseAddress = true
        s.broadcast = true
        s.sendBufferSize = SEND_BUFFER_BYTES
        s.bind(InetSocketAddress(port))
        socket = s
        running = true
        thread(name = "soundshare-control", isDaemon = true) { controlLoop(s) }
        if (beaconPort > 0) thread(name = "soundshare-beacon", isDaemon = true) { beaconLoop(s) }
    }

    /**
     * Sends one packet of audio to every listener. [pcm] must hold exactly
     * [Protocol.PACKET_PCM_BYTES]. Call from a single thread.
     */
    fun sendAudio(pcm: ByteArray) {
        val s = socket ?: return
        val thisSeq = seq++
        if (thisSeq % PRUNE_EVERY_PACKETS == 0) pruneClients()
        if (clients.isEmpty()) return
        Protocol.putAudioHeader(packet, sessionId, thisSeq)
        System.arraycopy(pcm, 0, packet, Protocol.AUDIO_HEADER_SIZE, Protocol.PACKET_PCM_BYTES)
        for (address in clients.keys) {
            try {
                s.send(DatagramPacket(packet, packet.size, address))
            } catch (_: IOException) {
                // A listener that cannot be reached right now is dropped by the timeout.
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        val s = socket ?: return
        // Network calls are not allowed on Android's main thread, where stop() may be called.
        thread(name = "soundshare-bye", isDaemon = true) {
            val bye = Protocol.control(Protocol.TYPE_BYE)
            for (address in clients.keys) {
                try {
                    s.send(DatagramPacket(bye, bye.size, address))
                } catch (_: IOException) {
                }
            }
            clients.clear()
            s.close()
        }
    }

    private fun controlLoop(s: DatagramSocket) {
        val buf = ByteArray(Protocol.MAX_PACKET_SIZE)
        val incoming = DatagramPacket(buf, buf.size)
        while (running) {
            try {
                incoming.length = buf.size
                s.receive(incoming)
            } catch (_: SocketException) {
                break
            } catch (_: IOException) {
                continue
            }
            when (Protocol.typeOf(buf, incoming.length)) {
                Protocol.TYPE_HELLO -> clients[incoming.socketAddress] = System.nanoTime()
                Protocol.TYPE_BYE -> clients.remove(incoming.socketAddress)
            }
        }
    }

    private fun beaconLoop(s: DatagramSocket) {
        val beacon = Protocol.encodeBeacon(sessionId, s.localPort, deviceName)
        while (running) {
            for (address in broadcastAddresses()) {
                try {
                    s.send(DatagramPacket(beacon, beacon.size, address, beaconPort))
                } catch (_: IOException) {
                }
            }
            try {
                Thread.sleep(Protocol.BEACON_INTERVAL_MS)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun pruneClients() {
        val cutoff = System.nanoTime() - Protocol.CLIENT_TIMEOUT_MS * 1_000_000
        clients.entries.removeAll { it.value < cutoff }
    }

    private companion object {
        const val PRUNE_EVERY_PACKETS = 200
        const val SEND_BUFFER_BYTES = 256 * 1024

        /** The broadcast address of every network this device is on, hotspot included. */
        fun broadcastAddresses(): List<InetAddress> = try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses.asSequence() }
                .mapNotNull { it.broadcast }
                .toList()
        } catch (_: SocketException) {
            emptyList()
        }
    }
}
