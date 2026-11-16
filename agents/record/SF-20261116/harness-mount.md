# Harness mount

- Kernel cloned from `StrawberryFrappe/agent-harness-kernel`, branch `testing`.
  Provenance is in `agents/local/`.
- Grill answers: solo owner with the listener as stakeholder; no AI attribution;
  paid app at 500 CLP; English harness; no Play account yet; hotspot before
  launch; visual direction undecided.
- Left out of the mount: translation protocol, SRS, 4+1 views (ADR 0001).
- Added project gates beyond the kit: GATE-RELEASE, GATE-DEVICE, GATE-L10N.
- `CLAUDE.md` holds only `@AGENTS.md`, so Claude Code reads the same rules.
- Known conflict recorded: the session's `claude/` branch prefix vs the
  no-attribution rule (ADR 0003, BL-010).
- Hook enabled in this clone with `git config core.hooksPath .githooks`.
- Owner proposed free listening and paid hosting. Compared one app with an
  in-app unlock against two apps (Lite and Pro); the owner's worry was losing
  hosting without internet. Neither option needs internet after purchase if the
  unlock is recorded on the device. Owner chose one app, a one-time unlock that
  works offline, and a free hosting trial: ADR 0008 supersedes ADR 0004.
- The first mount review was cut short by a container restart before it
  reported; rerun after ADR 0008.
