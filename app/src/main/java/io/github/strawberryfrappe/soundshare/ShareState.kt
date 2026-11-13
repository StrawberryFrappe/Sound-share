package io.github.strawberryfrappe.soundshare

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow

/** What the two background services are doing, for the screen to show. */
object ShareState {
    data class Sender(
        val running: Boolean = false,
        val listeners: Int = 0,
        @param:StringRes val error: Int? = null,
    )

    enum class Link { CONNECTING, PLAYING, NO_SIGNAL, SENDER_STOPPED }

    data class Receiver(
        val running: Boolean = false,
        val senderName: String = "",
        val link: Link = Link.CONNECTING,
        val bufferedMs: Int = 0,
        val dropouts: Long = 0,
        @param:StringRes val error: Int? = null,
    )

    const val MIN_DELAY_MS = 10
    const val MAX_DELAY_MS = 500
    const val DEFAULT_DELAY_MS = 40

    val sender = MutableStateFlow(Sender())
    val receiver = MutableStateFlow(Receiver())

    /** How long the listening phone holds audio before playing it. Changes apply while playing. */
    val delayMs = MutableStateFlow(DEFAULT_DELAY_MS)
}
