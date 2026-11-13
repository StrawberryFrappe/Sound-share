package io.github.strawberryfrappe.soundshare.sender

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState
import io.github.strawberryfrappe.soundshare.StreamingLocks
import io.github.strawberryfrappe.soundshare.core.Protocol
import io.github.strawberryfrappe.soundshare.core.StreamServer
import io.github.strawberryfrappe.soundshare.streamingNotification
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Runs on the device whose sound is shared. Captures what the device is
 * playing (Android's "playback capture", which needs the same consent as
 * screen recording) and hands it to a [StreamServer] 5 ms at a time.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class CaptureService : Service() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var projection: MediaProjection? = null
    private var record: AudioRecord? = null
    private var server: StreamServer? = null
    private var captureThread: Thread? = null
    private var locks: StreamingLocks? = null

    @Volatile
    private var running = false

    @Volatile
    @StringRes
    private var error: Int? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        // The user ended the capture from the system's own controls.
        override fun onStop() = stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY

        // Android requires the notification to be up before the capture consent is used.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            streamingNotification(getString(R.string.notification_sharing), stopIntent(this)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )

        val consent = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_CONSENT_DATA, Intent::class.java) }
        val resultCode = intent?.getIntExtra(EXTRA_CONSENT_CODE, 0) ?: 0
        if (consent == null || !startCapture(resultCode, consent)) {
            error = R.string.error_capture
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, consent: Intent): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) return false

        val record = try {
            val projection = getSystemService(MediaProjectionManager::class.java)
                .getMediaProjection(resultCode, consent) ?: return false
            this.projection = projection
            projection.registerCallback(projectionCallback, mainHandler)

            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(Protocol.SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
            val minBuffer = AudioRecord.getMinBufferSize(
                Protocol.SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT,
            )
            AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(maxOf(minBuffer, Protocol.PACKET_PCM_BYTES * 4))
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()
        } catch (_: SecurityException) {
            return false
        } catch (_: UnsupportedOperationException) {
            return false
        } catch (_: IllegalStateException) {
            return false
        }
        this.record = record

        val server = StreamServer(deviceName(this))
        this.server = server
        locks = StreamingLocks(this, "share").also { it.acquire() }
        running = true
        captureThread = thread(name = "soundshare-capture") { captureLoop(record, server) }
        return true
    }

    private fun captureLoop(record: AudioRecord, server: StreamServer) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        try {
            server.start()
            record.startRecording()
            ShareState.sender.value = ShareState.Sender(running = true)
            val pcm = ByteArray(Protocol.PACKET_PCM_BYTES)
            var packets = 0
            while (running) {
                var filled = 0
                while (filled < pcm.size) {
                    val read = record.read(pcm, filled, pcm.size - filled)
                    if (read <= 0) {
                        if (running) error = R.string.error_capture
                        return
                    }
                    filled += read
                }
                server.sendAudio(pcm)
                if (++packets % PACKETS_PER_STATUS == 0) {
                    val listeners = server.clientCount
                    if (ShareState.sender.value.listeners != listeners) {
                        ShareState.sender.value = ShareState.Sender(running = true, listeners = listeners)
                    }
                }
            }
        } catch (_: IOException) {
            error = R.string.error_port
        } catch (_: IllegalStateException) {
            error = R.string.error_capture
        } finally {
            if (running) mainHandler.post { stopSelf() }
        }
    }

    override fun onDestroy() {
        running = false
        try {
            record?.stop()
        } catch (_: IllegalStateException) {
        }
        captureThread?.join(500)
        record?.release()
        server?.stop()
        projection?.unregisterCallback(projectionCallback)
        projection?.stop()
        locks?.release()
        ShareState.sender.value = ShareState.Sender(running = false, error = error)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "io.github.strawberryfrappe.soundshare.STOP_SHARING"
        private const val EXTRA_CONSENT_CODE = "consent_code"
        private const val EXTRA_CONSENT_DATA = "consent_data"

        /** One status update per half second of audio. */
        private const val PACKETS_PER_STATUS = 100

        /** Starts sharing. [consentCode] and [consentData] are the result of the system's capture prompt. */
        fun start(context: Context, consentCode: Int, consentData: Intent) {
            ShareState.sender.value = ShareState.Sender()
            val intent = Intent(context, CaptureService::class.java)
                .putExtra(EXTRA_CONSENT_CODE, consentCode)
                .putExtra(EXTRA_CONSENT_DATA, consentData)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CaptureService::class.java))
        }

        private fun stopIntent(context: Context) =
            Intent(context, CaptureService::class.java).setAction(ACTION_STOP)

        /** The name the user gave this device, as the listener will see it. */
        fun deviceName(context: Context): String =
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?.takeIf { it.isNotBlank() }
                ?: Build.MODEL
    }
}
