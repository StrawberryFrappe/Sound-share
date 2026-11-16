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
