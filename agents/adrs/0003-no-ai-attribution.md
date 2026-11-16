# ADR 0003: No AI Attribution

## Status

Accepted by the owner on 2026-11-16.

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
- **Author identity:** commits up to `57ad5b1` are authored as the agent
  environment's identity. On 2026-11-16 the owner asked for their GitHub
  account, StrawberryFrappe, to be the author; later commits use that name with
  the account's GitHub noreply address, which links them to the account
  without publishing a personal email. Each agent environment sets this in its
  local git config. The hook checks messages only, not the author.
- **Known conflict (branch):** the current branch, `claude/android-audio-streaming-xmkci1`,
  has a `claude/` prefix, which the rule forbids. The cloud agent session that
  develops this project is required to push to that branch. It stays for now;
  release work moves to `main` (BL-010).

## Sources

- SRC-001 (owner answer, 2026-11-16), SRC-008 (kernel workflow)
- Mount review, `agents/reviews/20261116/harness-mount-review.md` (findings 1 and 2)
