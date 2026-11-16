# Project Profile

## Project Category

Consumer mobile app (Android), real-time media over the local network, sold on
Google Play. No backend.

## Maturity

**Early product.** The core works end to end (capture, encrypted UDP transport,
jitter buffering, playback, QR pairing, English and Spanish UI) and has been
installed and used by the owner on real devices up to 0.2.0. 0.3.0 has only been
exercised in emulators. Nothing is published, and the store requirements
(account, listing, policy declarations, closed test) are not started.

Broad refactors are not wanted; changes should be incremental.

## Primary Quality Risks

- **Real-device behaviour is the truth, and agents cannot observe it.** Audio
  quality, lip sync and Wi-Fi robustness can only be judged by the owner on
  real hardware. Emulators here run without hardware acceleration and without
  audio output.
- **Buyer expectations.** Netflix and similar apps cannot be shared. Unclear
  messaging means refunds and bad reviews; the free hosting trial (ADR 0008)
  lets people check first.
- **The unlock must never fail a paying user.** Losing hosting because the
  device is offline, or after a reinstall, would be the worst bug a buyer can
  hit (ADR 0008).
- **Security of a LAN stream.** Before 0.3.0 anyone on the network could
  listen. The pairing and encryption design (ADR 0006) must not regress.
- **Release build breakage.** R8 shrinking broke the QR scanner library once;
  release builds must be tested, not just debug builds.
- **Play policy compliance.** Microphone permission, media projection and
  foreground service types need declarations and a privacy policy at a public
  URL.
- **Signing key loss.** The release key lives outside git; losing it means
  updates cannot be published under the same listing.
- **Visual polish.** The owner wants stylistic changes; the direction is not
  decided yet.

## Active Gates

| Gate | Active | Reason |
|---|---|---|
| GATE-HM Harness Mounted | yes | Always required |
| GATE-LOCAL Local Mount | yes | Always required |
| GATE-DR Doctor | yes | Always required |
| GATE-TEST Tests | yes | Code project with a JVM test suite and lint |
| GATE-DEVICE Real Device | yes | Audio behaviour can only be verified on hardware |
| GATE-L10N App Strings | yes | App ships in English and Spanish |
| GATE-DEPLOY Deployment | yes | Google Play is the target; blocked until a developer account exists |
| GATE-PQ Portfolio Quality | yes | Paid, user-facing app with store screenshots |
| GATE-I18N Translation Currency | no | Harness docs are English only (ADR 0002) |

## Evidence Bar

- Unit tests and lint output for code changes.
- Release-build smoke run in an emulator for anything touching startup,
  libraries or shrinking.
- The owner's report from real devices for anything audible or visible.
- Screenshots from real devices for store and polish work (headless emulators
  here cannot capture the screen).

## Deployment Posture

Target: Google Play, Chile first, free with a one-time 500 CLP in-app unlock
for hosting (ADR 0008), as an Android App Bundle signed with the upload key. Blocked on the owner creating a developer account.
Until then, release APKs are delivered to the owner for sideloading.

## Collaboration Posture

Solo owner, with one stakeholder in mind (the listener). No other contributors,
so ADR and review files carry no author slugs (ADR 0002).
