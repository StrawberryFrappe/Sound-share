package io.github.strawberryfrappe.soundshare.core

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-GCM for single packets. The cleartext start of a packet (its header) is
 * authenticated but not encrypted; the rest is encrypted and followed by a
 * 16-byte tag. Every nonce must be used only once per key; [Protocol] says how
 * each kind of packet builds its nonce.
 */
object PacketCrypto {
    const val TAG_SIZE = 16
    const val NONCE_SIZE = 12

    private val ciphers = object : ThreadLocal<Cipher>() {
        override fun initialValue(): Cipher = Cipher.getInstance("AES/GCM/NoPadding")
    }

    /**
     * Encrypts [plainLength] bytes of [plain] into [packet] right after its first
     * [headerLength] bytes, which are authenticated as they are. Returns the
     * packet's total length.
     */
    fun seal(
        key: SecretKey,
        nonce: ByteArray,
        packet: ByteArray,
        headerLength: Int,
        plain: ByteArray,
        plainOffset: Int,
        plainLength: Int,
    ): Int {
        val cipher = ciphers.get()!!
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE * 8, nonce))
            cipher.updateAAD(packet, 0, headerLength)
            return headerLength + cipher.doFinal(plain, plainOffset, plainLength, packet, headerLength)
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException("AES-GCM is unavailable", e)
        }
    }

    /**
     * Checks and decrypts the [length]-byte [packet] whose first [headerLength]
     * bytes are its cleartext header. Writes the plaintext to [out] at
     * [outOffset] and returns its length, or -1 if the packet was not made with
     * this key and nonce or was altered on the way.
     */
    fun open(
        key: SecretKey,
        nonce: ByteArray,
        packet: ByteArray,
        headerLength: Int,
        length: Int,
        out: ByteArray,
        outOffset: Int,
    ): Int {
        if (length - headerLength < TAG_SIZE) return -1
        val cipher = ciphers.get()!!
        return try {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE * 8, nonce))
            cipher.updateAAD(packet, 0, headerLength)
            cipher.doFinal(packet, headerLength, length - headerLength, out, outOffset)
        } catch (_: GeneralSecurityException) {
            -1
        }
    }
}
