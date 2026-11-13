package io.github.strawberryfrappe.soundshare.core

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sends real UDP packets from a [StreamServer] to a [StreamClient] on this machine. */
class StreamLoopbackTest {
    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition()) {
            assertTrue("timed out waiting until $what", System.nanoTime() < deadline)
            Thread.sleep(10)
        }
    }

    @Test
    fun audioReachesAListenerThatSaidHello() {
        val server = StreamServer("test", port = 0, beaconPort = 0)
        server.start()
        val received = CopyOnWriteArrayList<Pair<Long, Byte>>()
        val senderStopped = CountDownLatch(1)
        val client = StreamClient(
            InetSocketAddress(InetAddress.getLoopbackAddress(), server.localPort),
            object : StreamClient.Listener {
                override fun onAudio(sessionId: Int, seq: Long, buf: ByteArray, offset: Int, length: Int) {
                    assertEquals(server.sessionId, sessionId)
                    assertEquals(Protocol.PACKET_PCM_BYTES, length)
                    received += seq to buf[offset]
                }

                override fun onSenderStopped() = senderStopped.countDown()
            },
        )
        client.start()
        try {
            waitUntil("the server knows the listener") { server.clientCount == 1 }
            for (i in 0 until 50) server.sendAudio(ByteArray(Protocol.PACKET_PCM_BYTES) { i.toByte() })
            waitUntil("all packets arrive") { received.size == 50 }
            assertEquals((0L until 50L).map { it to it.toByte() }, received.sortedBy { it.first })

            server.stop()
            assertTrue("listener should hear that the sender stopped", senderStopped.await(5, TimeUnit.SECONDS))
        } finally {
            client.stop()
            server.stop()
        }
    }

    @Test
    fun listenerIsForgottenWhenItLeaves() {
        val server = StreamServer("test", port = 0, beaconPort = 0)
        server.start()
        val client = StreamClient(
            InetSocketAddress(InetAddress.getLoopbackAddress(), server.localPort),
            object : StreamClient.Listener {
                override fun onAudio(sessionId: Int, seq: Long, buf: ByteArray, offset: Int, length: Int) {}
                override fun onSenderStopped() {}
            },
        )
        client.start()
        try {
            waitUntil("the server knows the listener") { server.clientCount == 1 }
            client.stop()
            waitUntil("the server forgets the listener") { server.clientCount == 0 }
        } finally {
            server.stop()
        }
    }

    @Test
    fun aBeaconPutsTheSenderOnTheDiscoveryList() {
        val lists = CopyOnWriteArrayList<List<SenderInfo>>()
        val discovery = BeaconListener(port = 0) { lists += it }
        discovery.start()
        try {
            waitUntil("discovery is listening") { discovery.localPort > 0 }
            val beacon = Protocol.encodeBeacon(7, 47800, "Tablet")
            val loopback = InetAddress.getLoopbackAddress()
            DatagramSocket().use { it.send(DatagramPacket(beacon, beacon.size, loopback, discovery.localPort)) }
            waitUntil("the sender is listed") { lists.isNotEmpty() }
            assertEquals(listOf(SenderInfo("Tablet", loopback, 47800)), lists.last())
        } finally {
            discovery.stop()
        }
    }
}
