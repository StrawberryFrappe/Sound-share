# ADR 0005: Stack

## Status

Proposed on 2026-11-16 from repository evidence (ASM-001); accepted when the
owner accepts the mounted harness.

## Context

The app was built before the harness existed. This records the stack as found,
so later changes are deliberate.

## Decision

- Kotlin, Jetpack Compose with Material 3, a single activity, two foreground
  services (capture and playback). No backend, no database.
- Gradle (wrapper), Android Gradle Plugin with R8 code and resource shrinking
  for release builds.
- Listening works on Android 6 (API 23) and newer; sharing needs Android 10
  (API 29), the first version with playback capture. The target SDK follows
  Google Play's current requirement.
- Uncompressed 48 kHz stereo PCM in 5 ms UDP packets, encrypted (ADR 0006).
  Compression (Opus) only if real-device loss shows the network cannot carry
  1.5 Mbit/s (BL-013).
- Libraries kept minimal: AndroidX, ZXing core (QR drawing), Google Play
  services code scanner (QR scanning without the camera permission), and Google
  Play Billing for the hosting unlock (ADR 0008).

Exact versions live in `gradle/libs.versions.toml` and
`agents/architecture/TECH_STACK.md`.

## Consequences

- One codebase for both roles.
- Releases depend on Google's Android toolchain being reachable from the build
  machine.
- Every new library must be checked in a **release** build: R8 already broke
  one (the QR scanner) in a way debug builds hid.

## Sources

- SRC-003 (code), SRC-004 (`README.md`)
