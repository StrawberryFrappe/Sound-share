package io.github.strawberryfrappe.soundshare.core

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketAddress
import java.net.SocketException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * The network half of the sending device.
 *
 * It streams encrypted audio to every paired listener that said hello in the
 * last few seconds, and broadcasts a beacon on every local network so
 * listeners can find this device without typing an address. A listener only
 * counts once its hello decrypts with this session's key, so devices without
 * the pairing code get nothing and cannot point the stream at anyone else.
 */
class StreamServer(
    private val deviceName: String,
    code: PairingCode,
    private val port: Int = Protocol.AUDIO_PORT,
    private val beaconPort: Int = Protocol.BEACON_PORT,
    random: SecureRandom = SecureRandom(),
) {
    private class Client(
        @Volatile var address: SocketAddress,
        val name: String,
        @Volatile var lastSeen: Long,
    )

    private val keyId = code.keyId
    private val sessionNonce = ByteArray(Protocol.SESSION_NONCE_SIZE).also(random::nextBytes)
    private val sessionTag = Protocol.sessionTag(sessionNonce)
    private val key = code.sessionKey(sessionNonce)

    private var socket: DatagramSocket? = null
    private val clients = ConcurrentHashMap<Long, Client>()

    /** The newest counter seen from every listener this session, kept after it leaves so replays stay useless. */
    private val lastCounters = ConcurrentHashMap<Long, Long>()
    private val packet = ByteArray(Protocol.AUDIO_PACKET_SIZE)
    private var seq = 0

    @Volatile
    private var running = false

    /** Names of the devices listening right now. */
    val listenerNames: List<String> get() = clients.values.map { it.name }.sorted()

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
        val length = Protocol.sealAudio(packet, key, sessionTag, thisSeq, pcm)
        for (client in clients.values) {
            try {
                s.send(DatagramPacket(packet, length, client.address))
            } catch (_: IOException) {
                // A listener that cannot be reached right now is dropped by the timeout.
            }
        }
    }

    fun stop() {
        if (!running) return
        running = false
        val s = socket ?: return
        val bye = Protocol.sealSenderBye(key, sessionTag, seq)
        // Network calls are not allowed on Android's main thread, where stop() may be called.
        thread(name = "soundshare-bye", isDaemon = true) {
            for (client in clients.values) {
                try {
                    s.send(DatagramPacket(bye, bye.size, client.address))
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
        val beacon = Protocol.encodeBeacon(keyId, sessionNonce, s.localPort, deviceName)
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
                Protocol.TYPE_PROBE -> if (incoming.length >= Protocol.PROBE_SIZE) {
                    try {
                        s.send(DatagramPacket(beacon, beacon.size, incoming.socketAddress))
                    } catch (_: IOException) {
                    }
                }
                Protocol.TYPE_HELLO, Protocol.TYPE_BYE -> {
                    val message = Protocol.openListenerMessage(buf, incoming.length, key) ?: continue
                    onListenerMessage(message, incoming.socketAddress)
                }
            }
        }
    }

    private fun onListenerMessage(message: Protocol.ListenerMessage, from: SocketAddress) {
        // An old message played back by someone else.
        val previous = lastCounters[message.listenerId]
        if (previous != null && message.counter <= previous) return
        lastCounters[message.listenerId] = message.counter

        val now = System.nanoTime()
        val client = clients[message.listenerId]
        if (client != null) {
            if (message.type == Protocol.TYPE_BYE) {
                clients.remove(message.listenerId)
                return
            }
            client.address = from
            client.lastSeen = now
        } else if (message.type == Protocol.TYPE_HELLO && clients.size < Protocol.MAX_LISTENERS) {
            clients[message.listenerId] = Client(from, message.name, now)
        }
    }

    private fun beaconLoop(s: DatagramSocket) {
        val beacon = Protocol.encodeBeacon(keyId, sessionNonce, s.localPort, deviceName)
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
        clients.entries.removeAll { it.value.lastSeen < cutoff }
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
