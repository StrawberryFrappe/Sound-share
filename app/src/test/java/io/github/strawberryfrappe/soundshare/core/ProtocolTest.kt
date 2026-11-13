package io.github.strawberryfrappe.soundshare.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolTest {
    @Test
    fun audioPacketFitsInOneWifiFrame() {
        assertEquals(960, Protocol.PACKET_PCM_BYTES)
        assertTrue(Protocol.AUDIO_PACKET_SIZE <= Protocol.MAX_PACKET_SIZE)
    }

    @Test
    fun audioHeaderRoundTrips() {
        val packet = ByteArray(Protocol.AUDIO_PACKET_SIZE)
        Protocol.putAudioHeader(packet, sessionId = -12345, seq = 0xFFFFFFFE.toInt())
        assertEquals(Protocol.TYPE_AUDIO, Protocol.typeOf(packet, packet.size))
        assertEquals(-12345, Protocol.audioSessionId(packet))
        assertEquals(0xFFFFFFFEL, Protocol.audioSeq(packet))
    }

    @Test
    fun beaconRoundTrips() {
        val packet = Protocol.encodeBeacon(sessionId = 42, port = 47800, name = "Tablet de Ana ☕")
        val beacon = Protocol.decodeBeacon(packet, packet.size)!!
        assertEquals(42, beacon.sessionId)
        assertEquals(47800, beacon.port)
        assertEquals("Tablet de Ana ☕", beacon.name)
    }

    @Test
    fun longNamesAreCutWithoutBreakingACharacter() {
        val packet = Protocol.encodeBeacon(1, 1, "é".repeat(40) + "😀".repeat(10))
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
    }
}
