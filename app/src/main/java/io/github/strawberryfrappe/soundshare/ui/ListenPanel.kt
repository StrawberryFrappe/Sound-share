package io.github.strawberryfrappe.soundshare.ui

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleStartEffect
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState
import io.github.strawberryfrappe.soundshare.core.BeaconListener
import io.github.strawberryfrappe.soundshare.core.Protocol
import io.github.strawberryfrappe.soundshare.core.SenderInfo
import io.github.strawberryfrappe.soundshare.receiver.PlaybackService
import kotlin.math.roundToInt

/** The tab for the phone with the headphones: pick a sharing device, then tune the delay. */
@Composable
fun ListenPanel(receiver: ShareState.Receiver, onDelayChosen: (Int) -> Unit) {
    if (receiver.running) {
        Listening(receiver, onDelayChosen)
    } else {
        ChooseSender(receiver)
    }
}

private class Target(val host: String, val port: Int, val name: String)

@Composable
private fun ChooseSender(receiver: ShareState.Receiver) {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences(MainActivity.SETTINGS, Context.MODE_PRIVATE) }
    var senders by remember { mutableStateOf(emptyList<SenderInfo>()) }
    var address by rememberSaveable {
        mutableStateOf(settings.getString(MainActivity.KEY_LAST_ADDRESS, "").orEmpty())
    }

    // Look for sharing devices only while this screen is visible.
    LifecycleStartEffect(Unit) {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // Many phones ignore broadcast packets unless an app asks for them.
        val broadcastLock = wifi.createMulticastLock("soundshare:discovery")
        broadcastLock.setReferenceCounted(false)
        broadcastLock.acquire()
        val listener = BeaconListener { senders = it }
        listener.start()
        onStopOrDispose {
            listener.stop()
            broadcastLock.release()
        }
    }

    // Ask for the notification permission on the way in, then connect whatever the answer.
    var pending by remember { mutableStateOf<Target?>(null) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        pending?.let { PlaybackService.start(context, it.host, it.port, it.name) }
        pending = null
    }
    fun connect(target: Target) {
        if (context.mayPostNotifications()) {
            PlaybackService.start(context, target.host, target.port, target.name)
        } else {
            pending = target
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun connectToTypedAddress() {
        val host = address.trim()
        if (host.isEmpty()) return
        settings.edit { putString(MainActivity.KEY_LAST_ADDRESS, host) }
        connect(Target(host, Protocol.AUDIO_PORT, host))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.listen_intro), style = MaterialTheme.typography.bodyLarge)
        receiver.error?.let { ErrorText(stringResource(it)) }

        if (senders.isEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.listen_searching), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text(stringResource(R.string.listen_found), style = MaterialTheme.typography.titleMedium)
            for (sender in senders) {
                val host = sender.address.hostAddress.orEmpty()
                Card(
                    onClick = { connect(Target(host, sender.port, sender.name)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(sender.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            host,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.listen_manual_title), style = MaterialTheme.typography.titleMedium)
            Hint(stringResource(R.string.listen_manual_hint))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(stringResource(R.string.listen_address_label)) },
                placeholder = { Text("192.168.1.20") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { connectToTypedAddress() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { connectToTypedAddress() },
                enabled = address.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.listen_connect))
            }
        }
    }
}

@Composable
private fun Listening(receiver: ShareState.Receiver, onDelayChosen: (Int) -> Unit) {
    val context = LocalContext.current
    var sliderMs by remember { mutableFloatStateOf(ShareState.delayMs.value.toFloat()) }
    val delayMs = (sliderMs / DELAY_STEP_MS).roundToInt() * DELAY_STEP_MS

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val status = when (receiver.link) {
                    ShareState.Link.CONNECTING -> R.string.link_connecting
                    ShareState.Link.PLAYING -> R.string.link_playing
                    ShareState.Link.NO_SIGNAL -> R.string.link_no_signal
                    ShareState.Link.SENDER_STOPPED -> R.string.link_sender_stopped
                }
                Text(stringResource(status, receiver.senderName), style = MaterialTheme.typography.titleLarge)
                if (receiver.link == ShareState.Link.PLAYING) {
                    val dropouts = receiver.dropouts.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    Hint(
                        stringResource(R.string.stats_buffered, receiver.bufferedMs) + "  ·  " +
                            pluralStringResource(R.plurals.stats_dropouts, dropouts, dropouts),
                    )
                }
            }
        }

        Column {
            Text(stringResource(R.string.delay_title, delayMs), style = MaterialTheme.typography.titleMedium)
            Slider(
                value = sliderMs,
                onValueChange = { sliderMs = it },
                // Applied on release: every change makes a tiny gap in the sound.
                onValueChangeFinished = { onDelayChosen(delayMs) },
                valueRange = ShareState.MIN_DELAY_MS.toFloat()..ShareState.MAX_DELAY_MS.toFloat(),
            )
            Hint(stringResource(R.string.delay_hint))
        }

        OutlinedButton(onClick = { PlaybackService.stop(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.listen_stop))
        }
    }
}

private const val DELAY_STEP_MS = 10
