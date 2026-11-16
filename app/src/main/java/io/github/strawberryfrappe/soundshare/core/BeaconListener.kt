package io.github.strawberryfrappe.soundshare.core

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

/** A device that is currently sharing its audio on this network. [keyId] tells which pairing code it uses. */
data class SenderInfo(val name: String, val address: InetAddress, val port: Int, val keyId: String)

/**
 * Listens for the beacons that sharing devices broadcast and reports the
 * current list every time it changes. A device that stops announcing itself
 * disappears from the list after a few seconds.
 */
class BeaconListener(
    private val port: Int = Protocol.BEACON_PORT,
    private val onChange: (List<SenderInfo>) -> Unit,
) {
    @Volatile
    private var socket: DatagramSocket? = null

    @Volatile
    private var running = false

    /** The port actually bound, or -1 until the listener is ready. */
    val localPort: Int get() = socket?.localPort ?: -1

    /**
     * Starts listening on a background thread. If the port cannot be opened
     * the list simply stays empty; the address can still be typed by hand.
     */
    fun start() {
        running = true
        thread(name = "soundshare-discovery", isDaemon = true) {
            val s = try {
                val unbound = DatagramSocket(null)
                unbound.reuseAddress = true
                unbound.soTimeout = RECEIVE_TIMEOUT_MS
                unbound.bind(InetSocketAddress(port))
                unbound
            } catch (_: IOException) {
                return@thread
            }
            socket = s
            if (running) listenLoop(s)
            s.close()
        }
    }

    fun stop() {
        running = false
        socket?.close()
    }

    private fun listenLoop(s: DatagramSocket) {
        val buf = ByteArray(Protocol.MAX_PACKET_SIZE)
        val incoming = DatagramPacket(buf, buf.size)
        val lastSeen = LinkedHashMap<SenderInfo, Long>()
        var reported = emptyList<SenderInfo>()
        while (running) {
            try {
                incoming.length = buf.size
                s.receive(incoming)
                val beacon = Protocol.decodeBeacon(buf, incoming.length)
                if (beacon != null) {
                    lastSeen[SenderInfo(beacon.name, incoming.address, beacon.port, beacon.keyId.toHex())] =
                        System.nanoTime()
                }
            } catch (_: SocketTimeoutException) {
            } catch (_: SocketException) {
                break
            } catch (_: IOException) {
            }
            val cutoff = System.nanoTime() - EXPIRY_MS * 1_000_000
            lastSeen.entries.removeAll { it.value < cutoff }
            val current = lastSeen.keys.toList()
            if (current != reported) {
                reported = current
                onChange(current)
            }
        }
    }

    private companion object {
        const val RECEIVE_TIMEOUT_MS = 500
        const val EXPIRY_MS = 4000L
    }
}
