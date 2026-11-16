# Project Brief

## Project Name

Sound Share (Android application id `io.github.strawberryfrappe.soundshare`).

## Product / System Intent

Let a second person hear what one Android device is playing, through their own
headphones on another Android device, over the local network.

The founding case: a tablet with no headphone jack shows a series to person A,
who listens on Bluetooth headphones; person B has a phone with a 3.5 mm jack and
wired headphones, and listens on the phone. The tablet shares its audio, the
phone plays it.

The same app runs on both devices: **Share audio** on the device with the
picture, **Listen** on the device with the headphones.

## Users / Audiences

- **The listener** (the person with the phone and wired headphones) is the
  stakeholder the owner keeps in mind. What matters to them: sound that does
  not cut out, sound that lines up with the picture, and setup that is quick.
- **The watcher** (the person with the tablet) starts sharing and should not
  have their watching disturbed.
- **People on Google Play in Chile**: the app is free to install and listen;
  hosting is a one-time 500 CLP in-app purchase (ADR 0008). Spanish-speaking
  users and store presentation matter.

## Success Criteria

- The app is published on Google Play, free to listen, with hosting unlocked
  by a one-time 500 CLP purchase that keeps working offline.
- On real devices, the listener hears the tablet's audio steadily and in sync
  with the picture, after a pairing step that takes seconds.
- Only paired devices can hear the stream (ADR 0006).
- Store listing and in-app text are honest about the limits, so buyers are not
  surprised (see Non-Goals).

## Non-Goals

- **Capturing apps that forbid capture.** Netflix and other DRM-protected apps
  opt out of Android playback capture; Sound Share does not try to get around
  that, by any means.
- **Bluetooth as the transport.** An Android phone cannot act as a Bluetooth
  headset for another device.
- **Internet streaming.** Sound never leaves the local network; there is no
  server.
- **iOS, desktop, or web clients.**

## Current Project State

- Type: early product, pre-release (version 0.3.0, versionCode 3).
- Existing code: Kotlin + Jetpack Compose Android app in `app/`. See
  `agents/architecture/TECH_STACK.md` and `agents/architecture/C4.md`.
- Existing docs/specs: `README.md` (user guide, build, design notes),
  `PRIVACY.md` (privacy policy for the store listing).
- Existing tests: JVM unit tests under `app/src/test/` (protocol, encryption,
  pairing, jitter buffer, loss concealment, real UDP loopback).
- Existing deployment: none. APKs are handed to the owner directly for
  sideloading. No Google Play developer account yet.

## Quality Bar

A paid, user-facing app: it must work on real devices and look credible.
GATE-PQ (portfolio quality), GATE-DEVICE (real-device check by the owner) and
GATE-L10N (English/Spanish parity) are active; see `agents/validation/GATES.md`.

## Open Questions

See `agents/intake/QUESTIONS_SUMMARY.md`.
