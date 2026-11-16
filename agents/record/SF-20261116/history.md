# Project history before the harness

Written at the mount from the agent session that built the app, so the story
survives outside one chat. Dates are the session's.

- **2026-11-13.** Owner's idea: person A watches on a tablet with no headphone
  jack and Bluetooth headphones; person B has a phone with a jack and wired
  headphones. Built the app: playback capture on the sender, 5 ms PCM over UDP,
  jitter buffer with clock-drift correction, beacon discovery. 0.1.0 debug APK,
  then an R8-shrunk signed release (11.8 MB to 1.3 MB). The release key was
  generated in the session and sent to the owner; it is not in git.
- **2026-11-13.** First install failed on the owner's LG K51S: an earlier debug
  build signed with a different key was still installed. Installing over it
  worked on an Android 11 emulator, so the cause was the signature, not the APK.
- **2026-11-13.** Owner: installation fine, connection "kinda iffy sometimes",
  maybe many Wi-Fi networks nearby. 0.2.0: default delay 40 to 120 ms,
  fade-based loss concealment, 250 ms capture and send buffers, "% lost" on
  screen.
- **2026-11-14.** Owner: Netflix does not work. Netflix opts out of playback
  capture; not something the app should get around. Removed a misleading
  "try a browser" tip.
- **2026-11-15.** Owner wants to sell on Google Play for 500 CLP, with
  stylistic changes, security, and maybe a hotspot flow. Found that anyone on
  the network could listen. 0.3.0: pairing code with QR, AES-GCM, replay
  protection, Spanish translation, privacy policy, backup exclusion. The QR
  scanner crashed inside Google's library in the release build only (R8); fixed
  with keep rules plus a fallback to typing the code. The emulator showed 27 %
  late audio after encryption; a JVM benchmark put AES-GCM at 1.8 µs per packet,
  so the likely cause is the emulator simulating AES instructions in software.
- **2026-11-16.** Owner asked to mount Agent Harness Kernel (branch `testing`).
