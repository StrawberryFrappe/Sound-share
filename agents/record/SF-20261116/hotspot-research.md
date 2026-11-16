# Hotspot research (BL-003, desk part)

Read the official Android developer pages on local-only hotspot, `WifiManager`,
Wi-Fi Direct, `WifiP2pConfig.Builder`, `WifiP2pManager`, Wi-Fi bootstrapping
(`WifiNetworkSpecifier`) and Wi-Fi permissions, on 2026-11-16, through a
page-reading subagent. What the pages say:

- **Local-only hotspot:** the network has no internet. STA+AP concurrency is a
  per-device capability (`WifiManager.isStaApConcurrencySupported()`). The pages
  read do not say whether the device's own Wi-Fi connection survives when the
  hotspot starts. Needs NEARBY_WIFI_DEVICES (target 33+) or fine location.
  A `startLocalOnlyHotspotWithConfiguration` method exists; its level and
  restrictions were not read.
- **Wi-Fi Direct:** since API 29, an app can create a group with a chosen
  network name (must start `DIRECT-xy`) and passphrase
  (`WifiP2pConfig.Builder`, `createGroup`). The group accepts legacy clients,
  which join it as a normal Wi-Fi network. Permissions: NEARBY_WIFI_DEVICES with
  `neverForLocation` on Android 13+; ACCESS_FINE_LOCATION below. Location Mode
  is required for peer discovery, not listed for `createGroup`. The docs do not
  say whether the device keeps its normal Wi-Fi connection while a group runs;
  they mention keeping the uplink over "mobile or any other available network".
  The group owner's address is not stated (192.168.49.1 is common, not
  documented).
- **Listener joining:** `WifiNetworkSpecifier` (API 29+) asks the user in a
  system dialog, remembered per app and network afterwards, and gives no
  internet. An app targeting SDK 29+ cannot add or connect to networks itself
  on any Android version (`addNetwork` and `enableNetwork` always fail), so
  listeners on Android 6 to 9 would join the network by hand in Settings.
- Not covered by the pages: binding the app's sockets to the joined network
  (`ConnectivityManager.bindProcessToNetwork` or `Network.bindSocket`), which is
  likely needed so the stream is not routed over another network.

Conclusion: nothing in the docs rules either approach out, and the deciding
question (does the tablet keep its own Wi-Fi?) can only be answered on the
owner's tablet. Proposed approach in ADR 0009.
