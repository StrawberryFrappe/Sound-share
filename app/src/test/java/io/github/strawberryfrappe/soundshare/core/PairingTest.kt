package io.github.strawberryfrappe.soundshare.core

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingTest {
    @Test
    fun codeIsFourGroupsOfFourAndReadsBack() {
        repeat(200) {
            val code = PairingCode.generate()
            assertTrue(code.text, Regex("[0-9A-HJKMNP-TV-Z]{4}(-[0-9A-HJKMNP-TV-Z]{4}){3}").matches(code.text))
            assertEquals(code, PairingCode.parse(code.text))
        }
    }

    @Test
    fun typingIsForgiving() {
        val code = PairingCode.generate()
        val sloppy = code.text.lowercase().replace("-", " ").replace('0', 'o').replace('1', 'l')
        assertEquals(code, PairingCode.parse(sloppy))
    }

    @Test
    fun aMistypedCharacterIsUsuallyCaught() {
        val random = SecureRandom()
        var caught = 0
        repeat(500) {
            val text = PairingCode.generate(random).text.replace("-", "").toCharArray()
            val i = random.nextInt(text.size)
            text[i] = if (text[i] == '7') '8' else '7'
            if (PairingCode.parse(String(text)) == null) caught++
        }
        // An 8-bit checksum lets about 1 in 256 slip through.
        assertTrue("caught $caught of 500", caught > 480)
    }

    @Test
    fun malformedCodesAreRejected() {
        assertNull(PairingCode.parse(""))
        assertNull(PairingCode.parse("ABCD-EFGH-JKMN"))
        assertNull(PairingCode.parse("ABCD-EFGH-JKMN-PQRS-TVWX"))
        assertNull(PairingCode.parse("ABCD-EFGH-JKMN-PQR!"))
    }

    @Test
    fun keysDependOnTheCodeAndTheSession() {
        val a = PairingCode.generate()
        val b = PairingCode.generate()
        val nonce1 = ByteArray(16) { 1 }
        val nonce2 = ByteArray(16) { 2 }
        assertTrue(a.sessionKey(nonce1).encoded.contentEquals(a.sessionKey(nonce1).encoded))
        assertFalse(a.sessionKey(nonce1).encoded.contentEquals(a.sessionKey(nonce2).encoded))
        assertFalse(a.sessionKey(nonce1).encoded.contentEquals(b.sessionKey(nonce1).encoded))
        assertFalse(a.keyId.contentEquals(b.keyId))
        assertEquals(16, a.sessionKey(nonce1).encoded.size)
    }

    @Test
    fun linkCarriesTheCodeAndAnAddressHint() {
        val code = PairingCode.generate()
        val link = PairingLink(code, "192.168.1.20", 47800)
        assertEquals(link, PairingLink.parse(link.encode()))
        assertEquals(PairingLink(code, null), PairingLink.parse(PairingLink(code, null).encode()))
        assertEquals(PairingLink(code, null), PairingLink.parse("soundshare:" + code.text))
        assertNull(PairingLink.parse("https://example.com"))
        assertNull(PairingLink.parse("SOUNDSHARE:not-a-code"))
        assertNotEquals(link.encode(), PairingLink(PairingCode.generate(), "192.168.1.20").encode())
    }
}
