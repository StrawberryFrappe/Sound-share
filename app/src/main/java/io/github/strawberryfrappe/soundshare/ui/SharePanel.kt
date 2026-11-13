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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState
import io.github.strawberryfrappe.soundshare.core.LocalAddresses
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
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
private fun Sharing(sender: ShareState.Sender) {
    val context = LocalContext.current
    val addresses by produceState(initialValue = emptyList<String>()) {
        while (true) {
            value = withContext(Dispatchers.IO) { LocalAddresses.privateIpv4() }
            delay(3000)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.share_running_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (sender.listeners == 0) {
                        stringResource(R.string.share_waiting)
                    } else {
                        pluralStringResource(R.plurals.share_listeners, sender.listeners, sender.listeners)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.share_device_name, CaptureService.deviceName(context)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (addresses.isEmpty()) {
                        stringResource(R.string.share_no_network)
                    } else {
                        stringResource(R.string.share_address, addresses.joinToString(" / "))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        OutlinedButton(onClick = { CaptureService.stop(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.share_stop))
        }
        Hint(stringResource(R.string.share_hint_volume))
        Hint(stringResource(R.string.share_hint_apps))
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
