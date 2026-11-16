package io.github.strawberryfrappe.soundshare.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    private val code = PairingCode.generate()
    private val nonce = ByteArray(Protocol.SESSION_NONCE_SIZE) { it.toByte() }
    private val key = code.sessionKey(nonce)
    private val listenerId = ByteArray(Protocol.LISTENER_ID_SIZE) { 7 }

    @Test
    fun everyPacketFitsInOneWifiFrame() {
        assertEquals(960, Protocol.PACKET_PCM_BYTES)
        assertTrue(Protocol.AUDIO_PACKET_SIZE <= Protocol.MAX_PACKET_SIZE)
        assertTrue(Protocol.PROBE_SIZE <= Protocol.MAX_PACKET_SIZE)
    }

    @Test
    fun audioRoundTripsAndIsNotReadable() {
        val pcm = ByteArray(Protocol.PACKET_PCM_BYTES) { (it * 31).toByte() }
        val packet = ByteArray(Protocol.AUDIO_PACKET_SIZE)
        val length = Protocol.sealAudio(packet, key, sessionTag = 99, seq = -2, pcm = pcm)
        assertEquals(Protocol.AUDIO_PACKET_SIZE, length)
        assertEquals(99, Protocol.packetSessionTag(packet))
        assertEquals(0xFFFFFFFEL, Protocol.packetSeq(packet))
        assertFalse("PCM must not travel in the clear", packet.copyOfRange(12, 12 + 960).contentEquals(pcm))

        val out = ByteArray(Protocol.PACKET_PCM_BYTES)
        assertTrue(Protocol.openAudio(packet, length, key, out))
        assertArrayEquals(pcm, out)
    }

    @Test
    fun alteredOrForeignAudioIsRejected() {
        val packet = ByteArray(Protocol.AUDIO_PACKET_SIZE)
        Protocol.sealAudio(packet, key, 1, 5, ByteArray(Protocol.PACKET_PCM_BYTES))
        val out = ByteArray(Protocol.PACKET_PCM_BYTES)

        val flippedSound = packet.copyOf().also { it[100] = (it[100].toInt() xor 1).toByte() }
        assertFalse(Protocol.openAudio(flippedSound, flippedSound.size, key, out))
        val changedSeq = packet.copyOf().also { it[11] = 6 }
        assertFalse("the header is authenticated too", Protocol.openAudio(changedSeq, changedSeq.size, key, out))
        assertFalse(Protocol.openAudio(packet, packet.size, PairingCode.generate().sessionKey(nonce), out))
        assertFalse(Protocol.openAudio(packet, packet.size, code.sessionKey(ByteArray(16)), out))
    }

    @Test
    fun listenerMessagesRoundTripAndAreTiedToTheKey() {
        val hello = Protocol.sealListenerMessage(Protocol.TYPE_HELLO, key, listenerId, 41, "Teléfono de Ana")
        val opened = Protocol.openListenerMessage(hello, hello.size, key)
        assertNotNull(opened)
        assertEquals(Protocol.TYPE_HELLO, opened!!.type)
        assertEquals(41, opened.counter)
        assertEquals("Teléfono de Ana", opened.name)
        assertEquals(0x07070707070707L, opened.listenerId)

        assertNull(Protocol.openListenerMessage(hello, hello.size, PairingCode.generate().sessionKey(nonce)))
        val asBye = hello.copyOf().also { it[3] = Protocol.TYPE_BYE.toByte() }
        assertNull("a hello cannot be turned into a goodbye", Protocol.openListenerMessage(asBye, asBye.size, key))
    }

    @Test
    fun senderGoodbyeIsAuthenticated() {
        val bye = Protocol.sealSenderBye(key, 3, 1000)
        assertTrue(Protocol.verifySenderBye(bye.copyOf(), bye.size, key))
        val forged = bye.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertFalse(Protocol.verifySenderBye(forged, forged.size, key))
    }

    @Test
    fun beaconRoundTrips() {
        val packet = Protocol.encodeBeacon(code.keyId, nonce, 47800, "Tablet de Ana ☕")
        val beacon = Protocol.decodeBeacon(packet, packet.size)!!
        assertArrayEquals(code.keyId, beacon.keyId)
        assertArrayEquals(nonce, beacon.sessionNonce)
        assertEquals(47800, beacon.port)
        assertEquals("Tablet de Ana ☕", beacon.name)
        assertTrue("a probe is never answered with more than it carried", packet.size <= Protocol.PROBE_SIZE)
    }

    @Test
    fun longNamesAreCutWithoutBreakingACharacter() {
        val packet = Protocol.encodeBeacon(code.keyId, nonce, 1, "é".repeat(40) + "😀".repeat(10))
        val beacon = Protocol.decodeBeacon(packet, packet.size)!!
        assertTrue(beacon.name.toByteArray().size <= Protocol.MAX_NAME_BYTES)
        assertTrue(beacon.name.none { it == '�' })
    }

    @Test
    fun foreignPacketsAreRejected() {
        val junk = "GET / HTTP/1.1".toByteArray()
        assertEquals(-1, Protocol.typeOf(junk, junk.size))
        assertNull(Protocol.decodeBeacon(junk, junk.size))
        assertEquals(-1, Protocol.typeOf(ByteArray(2), 2))
        val oldVersion = byteArrayOf('S'.code.toByte(), 'S'.code.toByte(), 1, 2)
        assertEquals("packets from the unencrypted version 1 are ignored", -1, Protocol.typeOf(oldVersion, 4))
    }
}
