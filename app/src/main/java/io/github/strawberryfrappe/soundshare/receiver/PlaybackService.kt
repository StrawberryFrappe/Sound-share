package io.github.strawberryfrappe.soundshare.receiver

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import androidx.annotation.StringRes
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState
import io.github.strawberryfrappe.soundshare.StreamingLocks
import io.github.strawberryfrappe.soundshare.core.JitterBuffer
import io.github.strawberryfrappe.soundshare.core.LossConcealer
import io.github.strawberryfrappe.soundshare.core.Protocol
import io.github.strawberryfrappe.soundshare.core.StreamClient
import io.github.strawberryfrappe.soundshare.streamingNotification
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import kotlin.concurrent.thread

/**
 * Runs on the listening phone. Receives the stream from the sharing device,
 * smooths it through a [JitterBuffer] and plays it on whatever this phone is
 * using for sound, normally its wired headphones.
 */
class PlaybackService : Service(), StreamClient.Listener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jitter = JitterBuffer(ShareState.DEFAULT_DELAY_MS / Protocol.PACKET_MS)
    private val concealer = LossConcealer()
    private var client: StreamClient? = null
    private var playThread: Thread? = null
    private var locks: StreamingLocks? = null
    private var focusRequest: AudioFocusRequest? = null
    private var senderName = ""

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var running = false

    @Volatile
    @StringRes
    private var error: Int? = null

    // Written by the network thread, read by the playback thread.
    @Volatile
    private var lastAudioNanos = 0L

    @Volatile
    private var senderStopped = false
    private var sessionId: Int? = null

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> track?.setVolume(1f)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> track?.setVolume(0.2f)
            // A phone call or a voice message: go quiet until it is over.
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> track?.setVolume(0f)
            // Another app started playing its own sound.
            AudioManager.AUDIOFOCUS_LOSS -> stopSelf()
        }
    }

    /** Headphones were unplugged: stop rather than blare out of the phone's speaker. */
    private val unpluggedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY

        val host = intent?.getStringExtra(EXTRA_HOST).orEmpty()
        val port = intent?.getIntExtra(EXTRA_PORT, Protocol.AUDIO_PORT) ?: Protocol.AUDIO_PORT
        senderName = intent?.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { host }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            streamingNotification(getString(R.string.notification_listening, senderName), stopIntent(this)),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        if (host.isEmpty()) {
            error = R.string.error_address
            stopSelf()
            return START_NOT_STICKY
        }

        locks = StreamingLocks(this, "listen").also { it.acquire() }
        requestAudioFocus()
        ContextCompat.registerReceiver(
            this,
            unpluggedReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        running = true
        publishStatus()
        playThread = thread(name = "soundshare-playback") { playLoop(host, port) }
        return START_NOT_STICKY
    }

    private fun playLoop(host: String, port: Int) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        try {
            val address = InetSocketAddress(InetAddress.getByName(host), port)
            val track = buildTrack()
            this.track = track
            track.play()
            client = StreamClient(address, this).also { it.start() }

            val out = ByteArray(jitter.maxPullBytes)
            var appliedDelayMs = -1
            var pulls = 0
            while (running) {
                val delayMs = ShareState.delayMs.value
                if (delayMs != appliedDelayMs) {
                    jitter.targetPackets = delayMs / Protocol.PACKET_MS
                    appliedDelayMs = delayMs
                }
                // write() waits until the phone has room for more sound, which paces this loop.
                val length = jitter.pull(out)
                concealer.process(out, length, jitter.lastPullHadAudio)
                if (track.write(out, 0, length) < 0) {
                    if (running) error = R.string.error_playback
                    return
                }
                if (++pulls % PULLS_PER_STATUS == 0) publishStatus()
            }
        } catch (_: UnknownHostException) {
            error = R.string.error_address
        } catch (_: IOException) {
            error = R.string.error_address
        } catch (_: IllegalStateException) {
            error = R.string.error_playback
        } catch (_: UnsupportedOperationException) {
            error = R.string.error_playback
        } finally {
            if (running) mainHandler.post { stopSelf() }
        }
    }

    private fun buildTrack(): AudioTrack {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(Protocol.SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()
        val minBuffer = AudioTrack.getMinBufferSize(
            Protocol.SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
        )
        val builder = AudioTrack.Builder()
            .setAudioAttributes(PLAYBACK_ATTRIBUTES)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, Protocol.PACKET_PCM_BYTES * 2))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        }
        return builder.build()
    }

    override fun onAudio(sessionId: Int, seq: Long, buf: ByteArray, offset: Int, length: Int) {
        if (sessionId != this.sessionId) {
            // The sharing device restarted its stream; its packet numbers start over.
            jitter.reset()
            this.sessionId = sessionId
        }
        jitter.push(seq, buf, offset, length)
        senderStopped = false
        lastAudioNanos = System.nanoTime()
    }

    override fun onSenderStopped() {
        senderStopped = true
    }

    private fun publishStatus() {
        val sinceAudioMs = (System.nanoTime() - lastAudioNanos) / 1_000_000
        val link = when {
            lastAudioNanos == 0L -> if (senderStopped) ShareState.Link.SENDER_STOPPED else ShareState.Link.CONNECTING
            sinceAudioMs < SIGNAL_TIMEOUT_MS -> ShareState.Link.PLAYING
            senderStopped -> ShareState.Link.SENDER_STOPPED
            else -> ShareState.Link.NO_SIGNAL
        }
        val stats = jitter.stats()
        ShareState.receiver.value = ShareState.Receiver(
            running = true,
            senderName = senderName,
            link = link,
            bufferedMs = stats.bufferedPackets * Protocol.PACKET_MS,
            dropouts = stats.underruns,
            lostPercent = if (stats.played == 0L) 0f else stats.lost * 100f / stats.played,
        )
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(PLAYBACK_ATTRIBUTES)
                .setOnAudioFocusChangeListener(focusListener, mainHandler)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun abandonAudioFocus() {
        val audioManager = getSystemService(AudioManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    override fun onDestroy() {
        if (running) {
            running = false
            client?.stop()
            playThread?.join(500)
            unregisterReceiver(unpluggedReceiver)
            abandonAudioFocus()
        }
        track?.release()
        locks?.release()
        ShareState.receiver.value = ShareState.Receiver(running = false, error = error)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2
        private const val ACTION_STOP = "io.github.strawberryfrappe.soundshare.STOP_LISTENING"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_PORT = "port"
        private const val EXTRA_NAME = "name"

        /** One status update per 200 ms of audio. */
        private const val PULLS_PER_STATUS = 40
        private const val SIGNAL_TIMEOUT_MS = 1500

        private val PLAYBACK_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

        /** Starts listening to the device at [host]. [name] is only for display. */
        fun start(context: Context, host: String, port: Int, name: String) {
            ShareState.receiver.value = ShareState.Receiver(running = true, senderName = name.ifBlank { host })
            val intent = Intent(context, PlaybackService::class.java)
                .putExtra(EXTRA_HOST, host)
                .putExtra(EXTRA_PORT, port)
                .putExtra(EXTRA_NAME, name)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }

        private fun stopIntent(context: Context) =
            Intent(context, PlaybackService::class.java).setAction(ACTION_STOP)
    }
}
