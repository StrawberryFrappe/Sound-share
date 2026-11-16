# Harness Mount Review

**Date:** 2026-11-16
**Reviewer:** an independent subagent (general-purpose, same model family as
the mounting agent), read-only, adversarial. Not a simulated role. A first
attempt was cut short by a container restart before it reported; this is the
second, complete run, done after ADR 0008.

## Verdict

APPROVE WITH FIXES. All findings below were addressed by the mounting agent
the same day, except where the resolution needs the owner (marked).

## Scope

Whether the project-specific harness is ready to govern implementation, and
whether its factual claims match the code.

## Verified Against The Code

- `agents/architecture/TECH_STACK.md` matches `gradle/libs.versions.toml` and
  `app/build.gradle.kts`.
- Every security claim in ADR 0006 holds in `core/` (replay counters kept after
  a listener leaves, nonce separation, probe size limit, version check, backup
  and device-transfer exclusion), with tests in `StreamLoopbackTest`.
- `agents/validation/TEST_STRATEGY.md` matches the 34 tests in `app/src/test/`.
- `agents/local/` is ignored (`git check-ignore`); no secrets or key material
  are committed.
- Every cited BL, ASM, EV, TR and SRC ID exists; status lives only in
  `agents/RUN_STATE.md`.

## Findings

| # | Severity | Finding | Resolution |
|---|---|---|---|
| 1 | P1 | All commits, including post-mount ones, are authored as the agent environment's identity, against "No AI Attribution"; the rule also forbids agents changing the identity | Recorded as a known conflict in ADR 0003 and as BL-019. **Owner decides** (QUESTIONS_SUMMARY) |
| 2 | P2 | The hook missed `Claude-Session:` trailers and cannot see the author | Pattern added to `.githooks/commit-msg` and tested; author not enforced, stated in ADR 0003 |
| 3 | P2 | ADR 0008 presented agent design choices as owner decisions | Split into owner decisions and a proposed design; confirmation question added. **Owner confirms** |
| 4 | P2 | Roadmap M7 still said "paid app" | Fixed |
| 5 | P2 | ADR 0005 listed Billing as if already in the build | Marked as a planned addition; version sources corrected |
| 6 | P2 | RUN_STATE claimed a doctor record that did not exist | Doctor run recorded in `agents/validation/DOCTOR.md` |
| 7 | P2 | Profile's gate table lacked GATE-RELEASE; GATE-PQ reason stale | Fixed |
| 8 | P2 | Environment truth (this machine's emulator, the cloud session's branch rule) in committed docs | Reworded to project-level statements; specifics stay in `agents/local/` |
| 9 | P3 | ADR 0006 overstated "cannot redirect the stream"; 32-bit sequence limit unstated | Limits added to ADR 0006 and ASM-005 |
| 10 | P3 | C4 omitted support files | Added |
| 11 | P3 | Style answer embellished | Marked as a proposed default |
| 12 | P3 | Trial default could become a requirement silently | BL-017 waits for the owner's answer |
| 13 | P3 | Some evidence rows were claims | EV-003 re-run and recorded; EV-002 and EV-004 labelled historical |
| 14 | P3 | Profile said real-device use "up to 0.2.0" | Corrected to 0.1.0, which the owner confirmed |
| 15 | P3 | Phone model named in a public record | Made generic |
| 16 | P3 | Local raw answers missed the pricing revision | Appended (local only) |

## Required Checks

- [x] Existing rules preserved and fused (`README.md`, `PRIVACY.md` referenced, not replaced).
- [x] Project profile populated beyond a broad category.
- [x] Source manifest populated.
- [x] Owner questions summarised; unanswered ones listed as open.
- [x] Assumptions visible.
- [x] ADRs for major setup decisions (0001 to 0008).
- [x] Validation gates fit the project and name evidence.
- [x] Capability scan completed (local).
- [x] Implementation gate explicit.
- [x] Local memory ignored by git.

## Decision

Implementation remains blocked until the owner accepts the mounted harness.
