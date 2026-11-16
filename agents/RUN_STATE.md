# Run State

The **single owner of status**. Replace, do not append. Every change here needs
a matching entry under `agents/record/`.

## Current Phase

Implementation. The owner accepted the harness on 2026-11-16 (EV-007) and asked
to be asked again later rather than now.

## Status

- App: 0.3.1 built, reviewed and smoke-tested (EV-008, EV-009), pushed as
  commit `c7c1e7f`, and handed to the owner. Adds the warning when the playing
  app blocks sharing (BL-012) and the string parity test (BL-015, done).
- Real-device checks pending, whenever the owner has time: pairing and sound
  on 0.3.x (BL-002); the blocked-app warning with Netflix (BL-012).
- CI (BL-014, done): GitHub Actions runs unit tests, lint and a release build on
  every push; the first two runs passed (EV-010).
- Private network (BL-003): desk research done; approach proposed in ADR 0009,
  waiting for the owner's tablet model and a decision on the location
  permission for Android 10 to 12.
- Hosting unlock (BL-016) and trial (BL-017): waiting for the trial length and
  for a Play test track.

## Next Action

Owner, when free: try 0.3.1 on both devices; answer the open P1 questions in
`agents/intake/QUESTIONS_SUMMARY.md` (trial length, tablet model, location
permission). Agent: continue with work that needs no owner input.

## Blockers

- Store work: no Play developer account (BL-005).
- BL-017 trial: length not decided.
- BL-003 private network: tablet model and permission decision.

## Last Verified State

2026-11-16: 0.3.1 release build: 43 unit tests pass, lint has no errors, the
release APK updates over 0.3.0 on an Android 11 emulator, keeps its pairing,
shares, and a remembered listener reconnects and plays.
