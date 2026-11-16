package io.github.strawberryfrappe.soundshare.core

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * The shared secret that pairs a listening phone with a sharing device.
 *
 * The sharing device makes one and keeps it until its user asks for a new
 * one. It is shown as a QR code and as 16 characters like `7K3M-QX9P-2RTD-W4HF`:
 * 72 random bits plus an 8-bit checksum that catches typing mistakes.
 *
 * Every sharing session derives a fresh encryption key from the secret and a
 * random session nonce, so nobody without the code can listen to, or inject
 * sound into, a stream.
 */
class PairingCode private constructor(private val secret: ByteArray) {

    /** The code as people read and type it, in four groups of four. */
    val text: String = encode(secret + checksum(secret)).chunked(4).joinToString("-")

    /** Identifies the code in broadcasts without revealing it, so a phone can recognise devices it paired with. */
    val keyId: ByteArray = prf(LABEL_KEY_ID, ByteArray(0)).copyOf(Protocol.KEY_ID_SIZE)

    /** The AES key for one sharing session. */
    fun sessionKey(sessionNonce: ByteArray): SecretKey =
        SecretKeySpec(prf(LABEL_SESSION, sessionNonce).copyOf(16), "AES")

    private fun prf(label: ByteArray, context: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        mac.update(label)
        return mac.doFinal(context)
    }

    override fun equals(other: Any?) = other is PairingCode && secret.contentEquals(other.secret)
    override fun hashCode() = secret.contentHashCode()
    override fun toString() = "PairingCode(${keyId.toHex()})"

    companion object {
        private const val SECRET_SIZE = 9
        private const val CODE_LENGTH = 16
        private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        private val LABEL_KEY_ID = "sound-share/key-id".toByteArray()
        private val LABEL_SESSION = "sound-share/session".toByteArray()

        fun generate(random: SecureRandom = SecureRandom()): PairingCode =
            PairingCode(ByteArray(SECRET_SIZE).also(random::nextBytes))

        /**
         * Reads a typed or scanned code. Case, dashes and spaces do not matter, and
         * letters that look like digits (O, I, L) are read as those digits.
         * Returns null when the code is malformed or its checksum does not match.
         */
        fun parse(text: String): PairingCode? {
            val cleaned = text.uppercase()
                .filterNot { it == '-' || it.isWhitespace() }
                .map { c ->
                    when (c) {
                        'O' -> '0'
                        'I', 'L' -> '1'
                        else -> c
                    }
                }
            if (cleaned.size != CODE_LENGTH || cleaned.any { it !in ALPHABET }) return null
            val bytes = decode(cleaned)
            val secret = bytes.copyOf(SECRET_SIZE)
            if (bytes[SECRET_SIZE] != checksum(secret)[0]) return null
            return PairingCode(secret)
        }

        private fun checksum(secret: ByteArray): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(secret).copyOf(1)

        /** Crockford base32 of exactly 80 bits. */
        private fun encode(bytes: ByteArray): String {
            val out = StringBuilder()
            var buffer = 0
            var bits = 0
            for (b in bytes) {
                buffer = (buffer shl 8) or (b.toInt() and 0xFF)
                bits += 8
                while (bits >= 5) {
                    bits -= 5
                    out.append(ALPHABET[(buffer shr bits) and 31])
                }
                buffer = buffer and ((1 shl bits) - 1)
            }
            return out.toString()
        }

        private fun decode(chars: List<Char>): ByteArray {
            val out = ByteArray(chars.size * 5 / 8)
            var buffer = 0
            var bits = 0
            var index = 0
            for (c in chars) {
                buffer = (buffer shl 5) or ALPHABET.indexOf(c)
                bits += 5
                if (bits >= 8) {
                    bits -= 8
                    out[index++] = (buffer shr bits).toByte()
                    buffer = buffer and ((1 shl bits) - 1)
                }
            }
            return out
        }
    }
}

/**
 * What the sharing device's QR code holds: the pairing code and, as a hint for
 * networks that block discovery, the address to reach it at.
 */
data class PairingLink(val code: PairingCode, val host: String?, val port: Int = Protocol.AUDIO_PORT) {
    fun encode(): String = PREFIX + code.text.replace("-", "") + (host?.let { "@$it:$port" } ?: "")

    companion object {
        private const val PREFIX = "SOUNDSHARE:"

        /** Reads a scanned QR code, or null if it did not come from Sound Share. */
        fun parse(text: String): PairingLink? {
            if (!text.startsWith(PREFIX, ignoreCase = true)) return null
            val rest = text.substring(PREFIX.length)
            val at = rest.indexOf('@')
            val code = PairingCode.parse(if (at < 0) rest else rest.substring(0, at)) ?: return null
            if (at < 0) return PairingLink(code, null)
            val address = rest.substring(at + 1)
            val colon = address.lastIndexOf(':')
            if (colon <= 0) return PairingLink(code, null)
            val port = address.substring(colon + 1).toIntOrNull()?.takeIf { it in 1..65535 }
                ?: return PairingLink(code, null)
            return PairingLink(code, address.substring(0, colon), port)
        }
    }
}

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }
