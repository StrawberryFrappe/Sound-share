# ADR 0003: No AI Attribution

## Status

Proposed on 2026-11-16; accepted when the owner accepts the mounted harness.

## Context

The kit's default rejects AI attribution in commits. The commits made before the
mount (up to `a9cefe7`) carry `Co-Authored-By` trailers naming an AI model,
added by the agent platform's default. The owner chose the kit's default.

## Decision

- From the mount onward, commits, PRs, tags, release notes and repository files
  carry no AI attribution (`agents/execution/WORKFLOW.md`, "No AI Attribution").
  This overrides any agent platform default.
- The `commit-msg` hook in `.githooks/` enforces it; each clone enables it with
  `git config core.hooksPath .githooks`.
- Past commits are not rewritten. Rewriting published history would break the
  existing branch for anyone who has it, and the owner did not ask for it.

## Consequences

- Agents must drop platform-added trailers and footers.
- **Known conflict:** the current branch, `claude/android-audio-streaming-xmkci1`,
  has a `claude/` prefix, which the rule forbids. The cloud agent session that
  develops this project is required to push to that branch. It stays for now;
  release work moves to `main` (BL-010).

## Sources

- SRC-001 (owner answer, 2026-11-16), SRC-008 (kernel workflow)
