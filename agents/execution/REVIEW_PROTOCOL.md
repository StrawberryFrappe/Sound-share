# Review Protocol

## Review Types

- Harness mount review
- Capability scan
- Maturity review
- Plan review
- Implementation review
- Adversarial review
- Final review

## Severity

- P0: blocking
- P1: blocking unless explicitly mitigated
- P2: should fix or document
- P3: optional improvement

## Capability Scans Are Per Developer

Review strength depends on what the reviewing agent can actually do, and that
differs per person and per machine. There is no project-wide answer, so the
harness does not record one.

- Each developer keeps their own scan at `agents/local/CAPABILITIES.md`, which is
  never committed. See `agents/LOCAL_SETUP.md`.
- Read your own scan before deciding how a review will be performed. Never assume
  a capability because a committed document, or another developer's review,
  mentions it.
- A dated capability scan under `agents/reviews/` is a historical record of one
  machine at one time. It is evidence about the past, not a claim about the
  current environment.

## Rules

- Use subagents for review when available.
- If subagents are unavailable, label the review as single-agent and record the
  downgrade in your local scan and in the review itself.
- Findings should cite files, commands, screenshots, or source evidence. A
  citation is checkable; an assertion is not.
- Do not count simulated roles as autonomous review.
- A blocking review finding must update `agents/RUN_STATE.md`.
- Small changes are reviewed and tested in batches, per phase or before a commit,
  merge or PR, not one pass each; see Token Budget And Review Cadence in
  `agents/execution/WORKFLOW.md`. All code is still reviewed before merge or ship.

## Project-Specific Review Points

- Anything touching `core/Protocol.kt`, `core/Pairing.kt` or `core/PacketCrypto.kt`
  gets an adversarial review against ADR 0006: nonce reuse, unauthenticated
  inputs, replay, amplification.
- Anything that changes what the app collects, stores or sends is checked
  against `PRIVACY.md`.
- UI changes are checked against GATE-L10N and GATE-PQ.

## When More Than One Developer Reviews

This project has one contributor (ADR 0002), so review files carry no author
slug. If a second contributor arrives, follow the naming rules in
`agents/execution/WORKFLOW.md`.
