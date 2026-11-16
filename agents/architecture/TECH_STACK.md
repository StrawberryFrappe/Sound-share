# Tech Stack

## Existing Stack

As found at the mount (0.3.0); the decision is ADR 0005. Exact versions are in
`gradle/libs.versions.toml` and `app/build.gradle.kts`, which win over this
table.

| Part | Choice |
|---|---|
| Language | Kotlin 2.4, Java 17 bytecode |
| UI | Jetpack Compose, Material 3 (follows the device's colours on Android 12+) |
| Build | Gradle wrapper 9.9, Android Gradle Plugin 9.5, R8 shrinking for release |
| Android levels | compile 37, target 36, min 23 (listening); sharing needs 29 |
| Libraries | AndroidX core, activity, lifecycle; ZXing core (draws the QR code); Google Play services code scanner (reads it) |
| Crypto | Platform `javax.crypto`: AES-128-GCM, HMAC-SHA256 (ADR 0006) |
| Network | UDP, ports 47800 (audio and control) and 47801 (discovery broadcast) |
| Audio | 48 kHz stereo 16-bit PCM, 5 ms packets; `AudioRecord` with playback capture, `AudioTrack` in low-latency mode |
| Tests | JUnit 4 on the JVM |
| Backend | None |

## Proposed / Accepted Stack

The existing stack, accepted by the owner (ADR 0005).
Decided addition: Google Play Billing Library for the hosting unlock
(ADR 0008, BL-016). Candidate additions, each needing its own decision: Opus
compression (BL-013), Wi-Fi Direct or local-only hotspot (ADR 0007, BL-003).

## Stack Decision Rules

- Respect the existing stack; it works.
- Every new dependency is checked in a release build (GATE-RELEASE).
- A new permission needs a store declaration and an update to `PRIVACY.md`.
- Record major stack decisions in ADRs.

## Deployment Target

Google Play, as an Android App Bundle signed with the upload key; free with
a one-time 500 CLP in-app unlock for hosting; Chile first (ADR 0008). Blocked until the developer account exists
(BL-005).
