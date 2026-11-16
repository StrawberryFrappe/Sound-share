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
- **Known conflict (author identity):** every commit so far, including those
  after the mount, is authored as `Claude <noreply@anthropic.com>`, the git
  identity configured in the cloud agent environment. The workflow requires the
  owner's own identity and also forbids agents from changing the configured
  identity, so this cannot be fixed by an agent alone. The owner either sets
  their own identity for agent work or accepts the conflict until BL-019 is
  done. The hook checks messages only, not the author.
- **Known conflict (branch):** the current branch, `claude/android-audio-streaming-xmkci1`,
  has a `claude/` prefix, which the rule forbids. The cloud agent session that
  develops this project is required to push to that branch. It stays for now;
  release work moves to `main` (BL-010).

## Sources

- SRC-001 (owner answer, 2026-11-16), SRC-008 (kernel workflow)
- Mount review, `agents/reviews/20261116/harness-mount-review.md` (findings 1 and 2)
