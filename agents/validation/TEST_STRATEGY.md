# Test Strategy

## Current Test Surface

JVM unit tests under `app/src/test/java/io/github/strawberryfrappe/soundshare/core/`:

- `ProtocolTest`: packet formats, encryption round trips, tamper and wrong-key
  rejection, beacon size limits.
- `PairingTest`: code format, typing tolerance, checksum, key derivation, QR
  payload.
- `StreamLoopbackTest`: real UDP on the loopback interface. A paired listener
  gets audio; a wrong code gets nothing; forged and replayed hellos are refused;
  short probes get no reply; a listener follows the sender into a new session.
- `JitterBufferTest`: ordering, loss, refill, drift correction under clock skew.
- `LossConcealerTest`: fades around gaps.

There are no instrumented (on-device) tests and no UI tests.

## Required Test Types

| Type | Applies When | Command / Method | Evidence |
|---|---|---|---|
| Unit | Any change to `core/` or logic | `./gradlew testDebugUnitTest` | Test report in `app/build/reports/tests/` |
| Static / lint | Any code or resource change | `./gradlew lintRelease` (errors fail the build) | `app/build/reports/lint-results-release.html` |
| Release build | Any change | `./gradlew assembleRelease bundleRelease` | APK and AAB under `app/build/outputs/` |
| Release smoke | GATE-RELEASE triggers | Install the release APK on an emulator (see `agents/local/CAPABILITIES.md` for how on this machine) and drive the flow with `adb` and `uiautomator` | Notes in `agents/validation/EVIDENCE_INDEX.md` |
| Real device | GATE-DEVICE | Owner installs the release APK on the tablet and phone and reports | Owner report |
| String parity | GATE-L10N | Compare string names in `values/` and `values-es/` | Check output |

## Known Limits Of Agent Testing Here

- Emulators run without hardware acceleration: slow, with CPU-bound figures
  (late audio, dropouts) that do not reflect real phones.
- Emulator images used so far have no Google Play services, so the QR scanner
  path cannot be exercised; only its fallback can.
- No audio output and no screenshots from the headless emulator.

## Rule

Do not invent test success. If tests cannot run, record why and what evidence
was used instead.
