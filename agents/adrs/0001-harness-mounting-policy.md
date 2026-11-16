# ADR 0001: Harness Mounting Policy

## Status

Proposed on 2026-11-16; accepted when the owner accepts the mounted harness.

## Context

The owner asked for the Agent Harness Kernel (branch `testing`) to be mounted
into Sound Share. Agent platforms do not share rules, memory or capabilities
across devices and tools, so a project-local harness makes the operating
contract travel with the repository.

## Decision

1. **The harness is the project-local `agents/` tree**, entered from the root
   `AGENTS.md`. `CLAUDE.md` contains only an import of `AGENTS.md`, so Claude
   Code finds the same instructions. Existing project documents (`README.md`,
   `PRIVACY.md`) are kept and referenced, not replaced.
2. **The harness is split into a shared half and a local half.** `agents/` is
   project truth and committed. `agents/local/` is environment truth
   (capabilities, machine paths, raw notes, kernel provenance) and is ignored by
   its own `.gitignore` (`*` and `!.gitignore`).
3. **The shared half is committed**, to a repository that is **public**.
4. **Mounted from the kit:** intake, planning, validation, execution, record,
   reviews, templates, `architecture/TECH_STACK.md` and `architecture/C4.md`.
   **Left out:** the translation protocol (docs are English only, ADR 0002),
   the SRS and the 4+1 views (the project is small; ADRs and the backlog carry
   intent).

## Consequences

- Any agent on any machine reads the same contract.
- Every clone must build `agents/local/` before implementation (GATE-LOCAL).
- Everything under `agents/` is readable by anyone on the internet. Candid
  assessments, owner details and anything about the signing key stay out of it.
- The SRS and 4+1 views can be mounted later from the kernel if the project
  grows.

## Sources

- SRC-001 (owner request, 2026-11-16), SRC-008 (kernel)
