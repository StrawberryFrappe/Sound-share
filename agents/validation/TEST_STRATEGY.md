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
- `SilenceWatchTest`: the "app may block sharing" warning appears after eight
  seconds of pure silence while something plays, and not otherwise.
- `StringResourcesTest` (package root): English and Spanish texts match by name
  and placeholders (GATE-L10N).

There are no instrumented (on-device) tests and no UI tests.

## Required Test Types

| Type | Applies When | Command / Method | Evidence |
|---|---|---|---|
| Unit | Any change to `core/` or logic | `./gradlew testDebugUnitTest` | Test report in `app/build/reports/tests/` |
| Static / lint | Any code or resource change | `./gradlew lintRelease` (errors fail the build) | `app/build/reports/lint-results-release.html` |
| Release build | Any change | `./gradlew assembleRelease bundleRelease` | APK and AAB under `app/build/outputs/` |
| Release smoke | GATE-RELEASE triggers | Install the release APK on an emulator (see `agents/local/CAPABILITIES.md` for how on this machine) and drive the flow with `adb` and `uiautomator` | Notes in `agents/validation/EVIDENCE_INDEX.md` |
| Real device | GATE-DEVICE | Owner installs the release APK on the tablet and phone and reports | Owner report |
| String parity | GATE-L10N | `StringResourcesTest`, run with the unit tests | Test report |
| Continuous integration | Every push and pull request | `.github/workflows/ci.yml`: unit tests, lint, release build | GitHub Actions run |

## Limits Of Agent Testing

- Agents cannot hear the audio or judge sync; that is GATE-DEVICE.
- Emulator performance figures (late audio, dropouts) say little about phones,
  especially without hardware acceleration.
- Emulator images without Google Play services cannot exercise the QR scanner,
  only its fallback; the in-app purchase needs a Play test track.
- Check your own `agents/local/CAPABILITIES.md` for what your emulator can do.

## Rule

Do not invent test success. If tests cannot run, record why and what evidence
was used instead.
