package io.github.strawberryfrappe.soundshare.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.strawberryfrappe.soundshare.PairingStore
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState
import io.github.strawberryfrappe.soundshare.core.LocalAddresses
import io.github.strawberryfrappe.soundshare.core.PairingLink
import io.github.strawberryfrappe.soundshare.deviceName
import io.github.strawberryfrappe.soundshare.sender.CaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The tab for the device with the picture: start and stop sending its sound. */
@Composable
fun SharePanel(sender: ShareState.Sender) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Text(stringResource(R.string.share_unsupported), style = MaterialTheme.typography.bodyLarge)
    } else if (sender.running) {
        Sharing(sender)
    } else {
        StartSharing(sender)
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun StartSharing(sender: ShareState.Sender) {
    val context = LocalContext.current
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    var confirmNewCode by rememberSaveable { mutableStateOf(false) }

    // Step 2: the system asks whether this app may capture the screen's sound.
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            CaptureService.start(context, result.resultCode, data)
        }
    }
    // Step 1: the permissions that capture needs.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionDenied = !context.hasPermission(Manifest.permission.RECORD_AUDIO)
        if (!permissionDenied) consentLauncher.launch(captureConsentIntent(context))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.share_intro), style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = {
                val permissions = buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(permissions.toTypedArray())
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.share_start))
        }
        if (permissionDenied) ErrorText(stringResource(R.string.share_permission_needed))
        sender.error?.let { ErrorText(stringResource(it)) }
        Hint(stringResource(R.string.share_hint_apps))

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.share_pairing_title), style = MaterialTheme.typography.titleMedium)
                Hint(stringResource(R.string.share_pairing_body))
                TextButton(onClick = { confirmNewCode = true }) {
                    Text(stringResource(R.string.share_new_code))
                }
            }
        }
    }

    if (confirmNewCode) {
        AlertDialog(
            onDismissRequest = { confirmNewCode = false },
            title = { Text(stringResource(R.string.share_new_code_title)) },
            text = { Text(stringResource(R.string.share_new_code_body)) },
            confirmButton = {
                TextButton(onClick = {
                    PairingStore(context).newSenderCode()
                    confirmNewCode = false
                }) {
                    Text(stringResource(R.string.share_new_code_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewCode = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun Sharing(sender: ShareState.Sender) {
    val context = LocalContext.current
    val code = remember { PairingStore(context).senderCode() }
    val addresses by produceState(initialValue = emptyList<String>()) {
        while (true) {
            value = withContext(Dispatchers.IO) { LocalAddresses.privateIpv4() }
            delay(3000)
        }
    }
    val link = PairingLink(code, addresses.firstOrNull()).encode()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.share_running_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (sender.listeners.isEmpty()) {
                        stringResource(R.string.share_waiting)
                    } else {
                        stringResource(R.string.share_listening_now, sender.listeners.joinToString(", "))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QrCode(link, Modifier.widthIn(max = 260.dp).fillMaxWidth())
                Text(stringResource(R.string.share_scan_hint), style = MaterialTheme.typography.bodyMedium)
                Text(
                    code.text,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                )
                Hint(
                    stringResource(R.string.share_device_name, deviceName(context)) + "\n" +
                        if (addresses.isEmpty()) {
                            stringResource(R.string.share_no_network)
                        } else {
                            stringResource(R.string.share_address, addresses.joinToString(" / "))
                        },
                )
            }
        }

        OutlinedButton(onClick = { CaptureService.stop(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.share_stop))
        }
        Hint(stringResource(R.string.share_hint_volume))
    }
}

/** Dark squares on white whatever the theme, which is what scanners expect. */
@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix = remember(content) {
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
    }
    val description = stringResource(R.string.qr_description)
    Canvas(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp)
            .semantics { contentDescription = description },
    ) {
        val cell = size.width / matrix.width
        // A hair of overlap keeps hairline gaps from showing between neighbouring squares.
        val square = Size(cell + 0.5f, cell + 0.5f)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), square)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun captureConsentIntent(context: Context): Intent {
    val manager = context.getSystemService(MediaProjectionManager::class.java)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        // Skip the "one app or whole screen" question: sound from any app should be shared.
        manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
    } else {
        manager.createScreenCaptureIntent()
    }
}
