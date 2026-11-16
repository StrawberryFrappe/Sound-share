package io.github.strawberryfrappe.soundshare.ui

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.LifecycleStartEffect
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.strawberryfrappe.soundshare.PairingStore
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState
import io.github.strawberryfrappe.soundshare.core.BeaconListener
import io.github.strawberryfrappe.soundshare.core.PairingCode
import io.github.strawberryfrappe.soundshare.core.PairingLink
import io.github.strawberryfrappe.soundshare.core.SenderInfo
import io.github.strawberryfrappe.soundshare.core.toHex
import io.github.strawberryfrappe.soundshare.receiver.PlaybackService
import kotlin.math.roundToInt

/** The tab for the phone with the headphones: pair with a sharing device, then tune the delay. */
@Composable
fun ListenPanel(receiver: ShareState.Receiver, onDelayChosen: (Int) -> Unit) {
    if (receiver.running) {
        Listening(receiver, onDelayChosen)
    } else {
        ChooseSender(receiver)
    }
}

private class Target(val host: String, val port: Int, val name: String, val code: PairingCode)

@Composable
private fun ChooseSender(receiver: ShareState.Receiver) {
    val context = LocalContext.current
    val store = remember { PairingStore(context) }
    val settings = remember { context.getSharedPreferences(MainActivity.SETTINGS, Context.MODE_PRIVATE) }
    var senders by remember { mutableStateOf(emptyList<SenderInfo>()) }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var codeText by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable {
        mutableStateOf(settings.getString(MainActivity.KEY_LAST_ADDRESS, "").orEmpty())
    }
    var message by remember { mutableStateOf<Int?>(null) }
    // A code typed without an address: connect as soon as its device shows up.
    var waitingFor by remember { mutableStateOf<PairingCode?>(null) }

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
        pending?.let { PlaybackService.start(context, it.host, it.port, it.name, it.code) }
        pending = null
    }
    fun connect(target: Target) {
        if (context.mayPostNotifications()) {
            PlaybackService.start(context, target.host, target.port, target.name, target.code)
        } else {
            pending = target
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun connect(sender: SenderInfo, code: PairingCode) =
        connect(Target(sender.address.hostAddress.orEmpty(), sender.port, sender.name, code))

    fun pair(link: PairingLink) {
        message = null
        store.remember(link.code)
        val found = senders.firstOrNull { it.keyId == link.code.keyId.toHex() }
        when {
            found != null -> connect(found, link.code)
            link.host != null -> connect(Target(link.host, link.port, link.host, link.code))
            else -> waitingFor = link.code
        }
    }
    fun scan() {
        message = null
        fun scannerUnavailable() {
            message = R.string.error_scanner
            showManual = true
        }
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        try {
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { barcode ->
                    val link = barcode.rawValue?.let(PairingLink::parse)
                    if (link == null) message = R.string.error_not_our_code else pair(link)
                }
                .addOnFailureListener { scannerUnavailable() }
        } catch (_: RuntimeException) {
            // The scanner comes from Google Play services, which some devices lack or have broken.
            scannerUnavailable()
        }
    }
    fun connectToTypedCode() {
        val code = PairingCode.parse(codeText)
        if (code == null) {
            message = R.string.error_code
            return
        }
        val host = address.trim().ifEmpty { null }
        settings.edit { putString(MainActivity.KEY_LAST_ADDRESS, host.orEmpty()) }
        pair(PairingLink(code, host))
    }

    LaunchedEffect(senders, waitingFor) {
        val code = waitingFor ?: return@LaunchedEffect
        senders.firstOrNull { it.keyId == code.keyId.toHex() }?.let {
            waitingFor = null
            connect(it, code)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.listen_intro), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = ::scan, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.listen_scan))
        }
        message?.let { ErrorText(stringResource(it)) }
        receiver.error?.let { ErrorText(stringResource(it)) }

        if (waitingFor != null) {
            Progress(R.string.listen_waiting_for_code)
            TextButton(onClick = { waitingFor = null }) { Text(stringResource(R.string.cancel)) }
        } else if (senders.isEmpty()) {
            Progress(R.string.listen_searching)
        } else {
            Text(stringResource(R.string.listen_nearby), style = MaterialTheme.typography.titleMedium)
            for (sender in senders) {
                val code = store.codeFor(sender.keyId)
                Card(
                    onClick = { if (code != null) connect(sender, code) else scan() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(sender.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(if (code != null) R.string.listen_paired_tap else R.string.listen_unpaired_tap),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (!showManual) {
            TextButton(onClick = { showManual = true }) { Text(stringResource(R.string.listen_enter_code)) }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Hint(stringResource(R.string.listen_manual_hint))
                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text(stringResource(R.string.listen_code_label)) },
                    placeholder = { Text("XXXX-XXXX-XXXX-XXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.listen_address_label)) },
                    placeholder = { Text("192.168.1.20") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { connectToTypedCode() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = ::connectToTypedCode,
                    enabled = codeText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.listen_connect))
                }
            }
        }
    }
}

@Composable
private fun Progress(@StringRes text: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
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
                    ShareState.Link.WRONG_CODE -> R.string.link_wrong_code
                }
                Text(stringResource(status, receiver.senderName), style = MaterialTheme.typography.titleLarge)
                if (receiver.link == ShareState.Link.PLAYING) {
                    val dropouts = receiver.dropouts.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    Hint(
                        stringResource(R.string.stats_buffered, receiver.bufferedMs) + "  ·  " +
                            pluralStringResource(R.plurals.stats_dropouts, dropouts, dropouts) + "  ·  " +
                            stringResource(R.string.stats_lost, "%.1f".format(receiver.lostPercent)),
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
