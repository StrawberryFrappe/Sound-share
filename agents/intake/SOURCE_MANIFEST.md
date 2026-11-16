# Source Manifest

| ID | Source | Type | Authority | Notes |
|---|---|---|---|---|
| SRC-001 | The owner's own words in agent sessions, starting 2026-11-13 | Conversation | Highest | Product idea, pricing, priorities. Summarised as accepted answers in `agents/intake/QUESTIONS_SUMMARY.md`; the raw answers are kept locally only. |
| SRC-002 | Accepted ADRs in `agents/adrs/` | Decision records | High | Durable decisions distilled from SRC-001. |
| SRC-003 | The code in `app/` and its tests in `app/src/test/` | Implementation | Describes what is, not what should be | When code and an ADR disagree, raise it; do not silently follow either. |
| SRC-004 | `README.md` | User and developer guide | Medium | Must stay true to the code; update it in the same change as the behaviour. |
| SRC-005 | `PRIVACY.md` | Published policy | Binding on the app | The app must not do anything this policy says it does not do. Changing data handling means changing this file first. |
| SRC-006 | Google Play policies (Developer Program Policies, Data safety, foreground service and permission declarations, target API level requirements) | External rules | Binding for publishing | Look them up when needed; they change. Do not rely on memory. |
| SRC-007 | Android platform documentation (playback capture, media projection, Wi-Fi APIs) | External reference | Reference | Check current docs for API-level behaviour. |
| SRC-008 | Agent Harness Kernel, branch `testing` | Setup material | Superseded by this harness | Provenance is recorded locally. Once mounted, this project's harness wins. |

## Authority Order

1. The owner's explicit decisions (SRC-001), as recorded in ADRs (SRC-002).
2. Binding external rules: Google Play policy (SRC-006) and the published
   privacy policy (SRC-005).
3. The code and tests (SRC-003), for how things currently work.
4. `README.md` (SRC-004).
5. Platform documentation (SRC-007).

## Source Handling Rules

- Do not rewrite original source documents unless explicitly asked.
- If extracted or summarized requirements conflict with original sources, record
  the conflict and ask for a decision.
- Derived requirements must be labeled and traceable to source or owner
  decisions. This project has no formal requirement IDs; trace decisions and
  outcomes instead (`agents/planning/TRACEABILITY.md`).
