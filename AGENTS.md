# Project Agent Instructions

Sound Share is an Android app that streams the sound one device is playing to
another device on the same network. This repository uses a mounted Agent
Harness Kernel under `agents/`, which is the operating contract for agentic
work here.

## Required Read Order

1. `agents/README.md`
2. `agents/RUN_STATE.md`
3. `agents/LOCAL_SETUP.md`, then your own `agents/local/CAPABILITIES.md`
4. `agents/intake/PROJECT_BRIEF.md`
5. `agents/intake/PROJECT_PROFILE.md`
6. `agents/intake/SOURCE_MANIFEST.md`
7. `agents/validation/GATES.md`
8. `agents/execution/WORKFLOW.md`
9. Latest relevant review under `agents/reviews/`

The harness documents are written in English only (ADR 0002). The app itself
ships in English and Spanish; keeping those two in step is GATE-L10N.

## The Local Half

`agents/` is project truth and is committed. What your agent can do, where your
checkout lives, and which binaries you invoke are **environment truth**, and
live in `agents/local/`, which is never committed.

A fresh clone therefore arrives incomplete on purpose. Build the local half
before implementation work: `agents/LOCAL_SETUP.md` says how, and GATE-LOCAL
enforces it.

## Core Rules

- Do not implement application code until the mounted harness is accepted,
  unless the owner explicitly overrides after risk disclosure. This gate is
  unconditional: it is not waived by small scope, high confidence, or complete
  context. Assumptions filled from context, even "safe" ones, must be surfaced
  for the owner to double-check before you proceed.
- Do not implement application code until `agents/local/CAPABILITIES.md` exists.
- Preserve existing project rules (`README.md`, `PRIVACY.md`) and fuse them into
  this file and `agents/`.
- Ask the owner when product, stack, deployment, evidence, or quality decisions
  are unclear.
- Decide review strength from your own capability scan. Do not assume a
  capability because a committed document mentions it.
- Keep environment truth and raw memory under `agents/local/`; never commit it.
- Never commit the release signing key or its passwords
  (`sound-share-release.jks`, `keystore.properties`); both are git-ignored.
- The repository is public. Everything committed under `agents/` is readable by
  anyone; write accordingly.
- Promote durable decisions into ADRs, planning docs, architecture docs,
  validation docs, or review summaries.
- If you detect drift from the harness, stop forward work and produce a handoff.
- Commits, PRs, branches, tags and repository files carry no AI attribution.
  See "No AI Attribution" in `agents/execution/WORKFLOW.md` and ADR 0003.
