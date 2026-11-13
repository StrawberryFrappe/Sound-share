# Sound Share

Watch on one Android device, listen on another.

Sound Share sends whatever one device is playing to a second device over
Wi‑Fi. The typical case: a tablet with no headphone jack shows the series, and
a phone with wired headphones plays the sound.

The same app runs on both devices. On the tablet choose **Share audio**; on the
phone choose **Listen**.

## Using it

1. Put both devices on the same network. Any of these works:
   - the same home Wi‑Fi,
   - a third phone's hotspot that both join,
   - the listening phone's own hotspot, joined by the tablet.
2. On the tablet: open Sound Share, **Share audio**, **Start sharing**. Android
   asks for the microphone permission and then shows its screen-capture
   prompt; accept both. (Android has no separate permission for "the sound
   this device is playing". The microphone itself is never used, and the
   picture is never captured.)
3. On the phone: open Sound Share, **Listen**, and tap the tablet when it
   appears. If it does not appear, type the address shown on the tablet.
4. Start the video on the tablet.

Things to know while watching:

- **Delay slider (phone).** Lower keeps the sound closer to the picture. Raise
  it if the sound cuts out. If the tablet is also playing to Bluetooth
  headphones, the video player delays the picture to match them, so the phone
  may be *early*: raise the delay until lips and voices line up.
- **Tablet volume.** The tablet keeps playing its own sound. Turn its volume
  down if only the phone should be heard; the shared sound stays at full level.
- **Phone volume** is the phone's normal media volume.
- Listening stops by itself when the headphones are unplugged.
- Several phones can listen to one tablet at the same time.

## Requirements and limits

- The sharing device needs Android 10 or newer. The listening device needs
  Android 6 or newer.
- Apps decide whether their sound may be captured. Some streaming apps
  (Netflix among them) forbid it, and then the phone stays silent. Playing the
  same service in a browser often works.
- Both devices must be able to reach each other. Guest and public Wi‑Fi often
  block that.
- Wi‑Fi only. An Android phone cannot act as a Bluetooth headset for another
  device, and Bluetooth is too slow to carry uncompressed sound by other means.

## Building

```sh
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest    # protocol, buffer and network tests
```

Needs JDK 17 or newer and the Android SDK (platform 37).

Release builds are optimized and shrunk with R8. They are signed with the key
named in `keystore.properties` at the project root:

```properties
storeFile=sound-share-release.jks
storePassword=...
keyAlias=soundshare
keyPassword=...
```

Neither file is in git. Keep both safe: Android only installs an update over
an existing install when it is signed with the same key. Without
`keystore.properties`, release builds are signed with the local debug key
instead, which is fine for trying things out.

## How it works

- **Capture** (`sender/CaptureService`): Android's playback-capture API
  records the device's media audio as 48 kHz stereo PCM.
- **Transport** (`core/`): the audio travels uncompressed in UDP packets of
  5 ms each (about 1.5 Mbit/s). The sharing device broadcasts a small beacon
  every second so listeners can find it; a listener says hello once a second
  and gets the stream for as long as it keeps doing so.
- **Playback** (`receiver/PlaybackService`): packets wait in a jitter buffer
  for the chosen delay, which absorbs uneven Wi‑Fi delivery, then play through
  a low-latency audio track. The buffer also plays imperceptibly faster or
  slower when the two devices' audio clocks drift apart.
