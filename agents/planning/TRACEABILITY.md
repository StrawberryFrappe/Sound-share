# Traceability

Links owner decisions to work, verification and evidence. This project has no
formal requirement IDs; it traces decisions and outcomes.

| Trace ID | Source / Decision | Backlog Item | Verification | Evidence |
|---|---|---|---|---|
| TR-001 | Kernel mount requested (SRC-001) | BL-001 | Harness mount review; doctor run | `agents/reviews/20261116/harness-mount-review.md`, `agents/validation/DOCTOR.md` |
| TR-002 | Share audio tablet to phone (Project Brief) | BL-002 | GATE-DEVICE owner check | `agents/validation/EVIDENCE_INDEX.md` |
| TR-003 | Only paired devices can listen (ADR 0006) | (done in 0.3.0) | `StreamLoopbackTest`, `ProtocolTest`, `PairingTest` (GATE-TEST) | EV-001 in `agents/validation/EVIDENCE_INDEX.md` |
| TR-004 | Paid app at 500 CLP (ADR 0004) | BL-005, BL-007 | Listing and pricing in the Play Console | Store listing URL (after M7) |
| TR-005 | Private network before launch (ADR 0007) | BL-003 | Spike results on the owner's devices | Follow-up ADR |
| TR-006 | Stylistic changes (owner goal) | BL-004 | GATE-PQ with real-device screenshots | `agents/validation/EVIDENCE_INDEX.md` |
| TR-007 | Honest limits: Netflix, both devices need the app (Non-Goals) | BL-007, BL-012 | Owner reviews the listing; device test of the warning | `agents/validation/EVIDENCE_INDEX.md` |
| TR-008 | English and Spanish app (Spanish added in 0.3.0) | BL-015 | GATE-L10N | EV-003 in `agents/validation/EVIDENCE_INDEX.md` |
| TR-009 | No AI attribution (ADR 0003) | BL-010 | `commit-msg` hook | `.githooks/commit-msg` |

## Rules

- Traceability grows as the project evolves.
- Do not expose internal trace IDs in the app or the store listing.
