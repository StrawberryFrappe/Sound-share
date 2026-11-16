package io.github.strawberryfrappe.soundshare

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow

/** What the two background services are doing, for the screen to show. */
object ShareState {
    data class Sender(
        val running: Boolean = false,
        /** Names of the devices listening right now. */
        val listeners: List<String> = emptyList(),
        @param:StringRes val error: Int? = null,
    )

    enum class Link { CONNECTING, PLAYING, NO_SIGNAL, SENDER_STOPPED, WRONG_CODE }

    data class Receiver(
        val running: Boolean = false,
        val senderName: String = "",
        val link: Link = Link.CONNECTING,
        val bufferedMs: Int = 0,
        val dropouts: Long = 0,
        val lostPercent: Float = 0f,
        @param:StringRes val error: Int? = null,
    )

    const val MIN_DELAY_MS = 10
    const val MAX_DELAY_MS = 500
    /**
     * Long enough to ride out the delivery hiccups of a busy Wi-Fi network. It also
     * roughly matches the delay of Bluetooth headphones, which the video player on the
     * sharing device already holds the picture back for.
     */
    const val DEFAULT_DELAY_MS = 120

    val sender = MutableStateFlow(Sender())
    val receiver = MutableStateFlow(Receiver())

    /** How long the listening phone holds audio before playing it. Changes apply while playing. */
    val delayMs = MutableStateFlow(DEFAULT_DELAY_MS)
}
