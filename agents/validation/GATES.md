# Validation Gates

## Required Gates

| Gate | Applies When | Pass Criteria | Evidence |
|---|---|---|---|
| GATE-HM Harness Mounted | Always | Harness accepted by the owner after review | `agents/reviews/20261116/harness-mount-review.md` plus the owner's acceptance |
| GATE-LOCAL Local Mount | Always | `agents/local/CAPABILITIES.md` exists and is current | The file itself |
| GATE-DR Doctor | Always | No unresolved hard blockers | `agents/validation/DOCTOR.md` |
| GATE-TEST Tests | Any code change | `./gradlew testDebugUnitTest lintRelease assembleRelease` passes, with no lint errors | Command output, recorded in `agents/validation/EVIDENCE_INDEX.md` for releases |
| GATE-RELEASE Release Smoke | Changes to startup, libraries, shrinking, permissions or services | The **release** APK installs and runs the changed flow in an emulator without crashing | Emulator run noted in `agents/validation/EVIDENCE_INDEX.md` |
| GATE-DEVICE Real Device | Anything the user hears or sees, before it is called done | The owner runs the build on real devices and reports the result | Owner's report in `agents/validation/EVIDENCE_INDEX.md` |
| GATE-L10N App Strings | Any change to app text | Every string in `app/src/main/res/values/strings.xml` has a Spanish version in `app/src/main/res/values-es/strings.xml`, and the reverse | Parity check output (BL-015 automates it) |
| GATE-DEPLOY Deployment | Store releases | The bundle is accepted by the Play Console on the intended track, or the blocker is recorded | Play Console state; until an account exists, the blocker in `agents/RUN_STATE.md` |
| GATE-PQ Portfolio Quality | User-facing changes and store assets | Visible output is credible, polished and demonstrable | Real-device screenshots approved by the owner |

GATE-I18N (harness translation currency) is inactive: harness documents are
English only (ADR 0002).

## Why GATE-RELEASE And GATE-DEVICE Exist

- R8 shrinking broke the QR scanner library in 0.3.0 in a way only the release
  build showed (a crash inside the library, caught in the emulator). Debug-only
  testing is not enough.
- Audio quality, lip sync and Wi-Fi behaviour cannot be observed by an agent:
  the build machine's emulators have no audio output, no hardware acceleration,
  and cannot capture the screen. Only the owner's devices can pass GATE-DEVICE.

## Local Mount Gate

Implementation work requires the local half of the harness to exist. Reading
documents and making trivial corrections are not blocked. Setup instructions:
`agents/LOCAL_SETUP.md`.

## Portfolio Quality Gate

The project fails this gate if it looks technically present but embarrassing to
show:

- scaffold-looking screens or placeholder copy;
- text that describes implementation instead of what the user gets;
- missing states: searching, nothing found, error, connected, lost connection;
- controls that do not map to tested behaviour;
- store screenshots that do not show real use.

## Rules

- Passing tests are not enough when the visible product is weak.
- Every gate names its evidence. A gate whose evidence is a claim rather than an
  artifact has not been passed.
