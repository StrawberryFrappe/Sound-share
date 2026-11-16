package io.github.strawberryfrappe.soundshare

import android.content.Context
import android.os.Build
import android.provider.Settings

/** The name the user gave this device, as other devices will show it. */
fun deviceName(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }
    return Build.MODEL
}
