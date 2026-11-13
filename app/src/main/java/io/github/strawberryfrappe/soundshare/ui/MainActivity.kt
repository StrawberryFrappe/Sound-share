package io.github.strawberryfrappe.soundshare.ui

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.strawberryfrappe.soundshare.R
import io.github.strawberryfrappe.soundshare.ShareState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The volume keys control what the headphones play, even before any sound has started.
        volumeControlStream = AudioManager.STREAM_MUSIC

        val settings = getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
        ShareState.delayMs.value = settings.getInt(KEY_DELAY_MS, ShareState.DEFAULT_DELAY_MS)
            .coerceIn(ShareState.MIN_DELAY_MS, ShareState.MAX_DELAY_MS)

        setContent {
            SoundShareTheme {
                SoundShareApp(
                    onDelayChosen = { delayMs ->
                        ShareState.delayMs.value = delayMs
                        settings.edit { putInt(KEY_DELAY_MS, delayMs) }
                    },
                )
            }
        }
    }

    companion object {
        const val SETTINGS = "settings"
        const val KEY_DELAY_MS = "delay_ms"
        const val KEY_LAST_ADDRESS = "last_address"
    }
}

private enum class Tab(@param:StringRes val label: Int) {
    SHARE(R.string.tab_share),
    LISTEN(R.string.tab_listen),
}

@Composable
private fun SoundShareApp(onDelayChosen: (Int) -> Unit) {
    val sender by ShareState.sender.collectAsStateWithLifecycle()
    val receiver by ShareState.receiver.collectAsStateWithLifecycle()

    // Open on whatever this device is already doing; otherwise guess from its size:
    // the tablet is the one with the picture, the phone is the one with the headphones.
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    var tab by rememberSaveable {
        mutableStateOf(
            when {
                sender.running -> Tab.SHARE
                receiver.running -> Tab.LISTEN
                isTablet -> Tab.SHARE
                else -> Tab.LISTEN
            },
        )
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Tab.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            shape = SegmentedButtonDefaults.itemShape(index, Tab.entries.size),
                        ) {
                            Text(stringResource(entry.label))
                        }
                    }
                }
                when (tab) {
                    Tab.SHARE -> SharePanel(sender)
                    Tab.LISTEN -> ListenPanel(receiver, onDelayChosen)
                }
            }
        }
    }
}
