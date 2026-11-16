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

/** Real UDP between a [StreamServer] and [StreamClient]s on this machine, including devices without the code. */
class StreamLoopbackTest {
    private val loopback = InetAddress.getLoopbackAddress()

    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        while (!condition()) {
            assertTrue("timed out waiting until $what", System.nanoTime() < deadline)
            Thread.sleep(10)
        }
    }

    private class Recorder : StreamClient.Listener {
        val audio = CopyOnWriteArrayList<Pair<Long, Byte>>()
        val stopped = CountDownLatch(1)
        val wrongCode = CountDownLatch(1)
        override fun onAudio(sessionTag: Int, seq: Long, buf: ByteArray, offset: Int, length: Int) {
            assertEquals(Protocol.PACKET_PCM_BYTES, length)
            audio += seq to buf[offset]
        }
        override fun onSenderStopped() = stopped.countDown()
        override fun onWrongCode() = wrongCode.countDown()
    }

    private fun server(code: PairingCode) = StreamServer("Tablet", code, port = 0, beaconPort = 0).also { it.start() }

    private fun client(server: StreamServer, code: PairingCode, recorder: Recorder, name: String = "Phone") =
        StreamClient(InetSocketAddress(loopback, server.localPort), code, name, recorder).also { it.start() }

    @Test
    fun aPairedListenerGetsTheAudio() {
        val code = PairingCode.generate()
        val server = server(code)
        val recorder = Recorder()
        val client = client(server, code, recorder, name = "LG K51S")
        try {
            waitUntil("the server knows the listener") { server.clientCount == 1 }
            assertEquals(listOf("LG K51S"), server.listenerNames)
            for (i in 0 until 50) server.sendAudio(ByteArray(Protocol.PACKET_PCM_BYTES) { i.toByte() })
            waitUntil("all packets arrive") { recorder.audio.size == 50 }
            assertEquals((0L until 50L).map { it to it.toByte() }, recorder.audio.sortedBy { it.first })

            server.stop()
            assertTrue("listener should hear that the sender stopped", recorder.stopped.await(5, TimeUnit.SECONDS))
        } finally {
            client.stop()
            server.stop()
        }
    }

    @Test
    fun aListenerWithTheWrongCodeGetsNothingAndIsTold() {
        val server = server(PairingCode.generate())
        val recorder = Recorder()
        val client = client(server, PairingCode.generate(), recorder)
        try {
            assertTrue(recorder.wrongCode.await(5, TimeUnit.SECONDS))
            repeat(100) { server.sendAudio(ByteArray(Protocol.PACKET_PCM_BYTES)) }
            Thread.sleep(300)
            assertEquals(0, server.clientCount)
            assertTrue(recorder.audio.isEmpty())
        } finally {
            client.stop()
            server.stop()
        }
    }

    @Test
    fun forgedAndReplayedHellosAreIgnored() {
        val code = PairingCode.generate()
        val server = server(code)
        DatagramSocket().use { attacker ->
            attacker.soTimeout = 2000
            val serverAddress = InetSocketAddress(loopback, server.localPort)
            fun send(bytes: ByteArray) = attacker.send(DatagramPacket(bytes, bytes.size, serverAddress))

            // The probe answer is public: it tells the session, not the key.
            send(Protocol.probe())
            val buf = ByteArray(Protocol.MAX_PACKET_SIZE)
            val reply = DatagramPacket(buf, buf.size)
            attacker.receive(reply)
            val beacon = Protocol.decodeBeacon(buf, reply.length)!!

            // Without the code, a hello cannot be made.
            val guessedKey = PairingCode.generate().sessionKey(beacon.sessionNonce)
            send(Protocol.sealListenerMessage(Protocol.TYPE_HELLO, guessedKey, ByteArray(7), 1, "Intruder"))
            Thread.sleep(300)
            assertEquals(0, server.clientCount)

            // A real listener's hello, captured and played back after it left, is refused too.
            val realKey = code.sessionKey(beacon.sessionNonce)
            val hello = Protocol.sealListenerMessage(Protocol.TYPE_HELLO, realKey, ByteArray(7) { 1 }, 1, "Phone")
            send(hello)
            waitUntil("the real hello is accepted") { server.clientCount == 1 }
            send(Protocol.sealListenerMessage(Protocol.TYPE_BYE, realKey, ByteArray(7) { 1 }, 2, "Phone"))
            waitUntil("the listener leaves") { server.clientCount == 0 }
            send(hello)
            Thread.sleep(300)
            assertEquals("replayed hello must not restart the stream", 0, server.clientCount)

            // A tiny probe, which could be used to make the sender shout at someone else, gets no answer.
            send(Protocol.probe().copyOf(Protocol.HEADER_SIZE))
            attacker.soTimeout = 500
            val silent = runCatching { attacker.receive(reply) }.isFailure
            assertTrue("short probes are ignored", silent)
        }
        server.stop()
    }

    @Test
    fun listenerIsForgottenWhenItLeaves() {
        val code = PairingCode.generate()
        val server = server(code)
        val client = client(server, code, Recorder())
        try {
            waitUntil("the server knows the listener") { server.clientCount == 1 }
            client.stop()
            waitUntil("the server forgets the listener") { server.clientCount == 0 }
        } finally {
            server.stop()
        }
    }

    @Test
    fun aListenerFollowsTheSenderIntoANewSession() {
        val code = PairingCode.generate()
        val first = server(code)
        val port = first.localPort
        val recorder = Recorder()
        val client = StreamClient(InetSocketAddress(loopback, port), code, "Phone", recorder).also { it.start() }
        try {
            waitUntil("first session connects") { first.clientCount == 1 }
            first.stop()
            assertTrue(recorder.stopped.await(5, TimeUnit.SECONDS))
            Thread.sleep(200)

            val second = StreamServer("Tablet", code, port = port, beaconPort = 0).also { it.start() }
            try {
                waitUntil("the listener rejoins the new session") { second.clientCount == 1 }
                repeat(5) { second.sendAudio(ByteArray(Protocol.PACKET_PCM_BYTES)) }
                waitUntil("audio from the new session arrives") { recorder.audio.size == 5 }
            } finally {
                second.stop()
            }
        } finally {
            client.stop()
        }
    }

    @Test
    fun aBeaconPutsTheSenderOnTheDiscoveryList() {
        val lists = CopyOnWriteArrayList<List<SenderInfo>>()
        val discovery = BeaconListener(port = 0) { lists += it }
        discovery.start()
        try {
            waitUntil("discovery is listening") { discovery.localPort > 0 }
            val code = PairingCode.generate()
            val beacon = Protocol.encodeBeacon(code.keyId, ByteArray(16), 47800, "Tablet")
            DatagramSocket().use { it.send(DatagramPacket(beacon, beacon.size, loopback, discovery.localPort)) }
            waitUntil("the sender is listed") { lists.isNotEmpty() }
            assertEquals(listOf(SenderInfo("Tablet", loopback, 47800, code.keyId.toHex())), lists.last())
        } finally {
            discovery.stop()
        }
    }
}
