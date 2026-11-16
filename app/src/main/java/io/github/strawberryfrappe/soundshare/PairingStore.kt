package io.github.strawberryfrappe.soundshare

import android.content.Context
import androidx.core.content.edit
import io.github.strawberryfrappe.soundshare.core.PairingCode
import io.github.strawberryfrappe.soundshare.core.toHex

/**
 * Keeps this device's own pairing code (for sharing) and the codes of the
 * devices it has paired with (for listening). Stored in app-private storage
 * and left out of backups.
 */
class PairingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The code listeners pair with to hear this device. Made the first time it is needed. */
    fun senderCode(): PairingCode =
        prefs.getString(KEY_SENDER_CODE, null)?.let(PairingCode::parse) ?: newSenderCode()

    /** Replaces this device's code, so everyone paired with the old one must pair again. */
    fun newSenderCode(): PairingCode {
        val code = PairingCode.generate()
        prefs.edit { putString(KEY_SENDER_CODE, code.text) }
        return code
    }

    fun remember(code: PairingCode) = prefs.edit { putString(PAIRED_PREFIX + code.keyId.toHex(), code.text) }

    fun forget(code: PairingCode) = prefs.edit { remove(PAIRED_PREFIX + code.keyId.toHex()) }

    /** The code of a paired device, by the key id it broadcasts. */
    fun codeFor(keyId: String): PairingCode? =
        prefs.getString(PAIRED_PREFIX + keyId, null)?.let(PairingCode::parse)

    private companion object {
        const val FILE = "pairing"
        const val KEY_SENDER_CODE = "sender_code"
        const val PAIRED_PREFIX = "paired_"
    }
}
