# Roadmap

## Current Goal

Launch Sound Share on Google Play: free to listen, hosting unlocked by a
one-time 500 CLP purchase (ADR 0008), with a listener experience that works on
real devices, secure pairing, a polished look, and a private-network option.

## Milestones

| Milestone | Outcome | Evidence |
|---|---|---|
| M1 Harness | The harness is accepted and governs further work | BL-001 |
| M2 Verified core | 0.3.x pairs and plays steadily on the owner's tablet and phone | BL-002 in `agents/validation/EVIDENCE_INDEX.md` |
| M3 Private network | Pairing works without a shared Wi-Fi network, on the owner's devices | BL-003 follow-up ADR and owner test |
| M3b Hosting unlock | Free listening; trial and one-time unlock for hosting that keep working offline | BL-016, BL-017 |
| M4 Look and feel | Visual direction chosen and applied; store screenshots exist | BL-004; GATE-PQ |
| M5 Store ready | Account, payments, signing, listing, privacy URL and declarations in place; in-app product created; release bundle built from `main` | BL-005, BL-007 to BL-011, BL-018 |
| M6 Closed test | 12 or more testers for 14 days, the listener stakeholder included; feedback addressed | BL-006 |
| M7 Launch | Free app with the hosting unlock live on Google Play in Chile | Store listing URL |

M2 can run alongside M1. M3, M3b and M4 can run in parallel once M2 is done;
M3b's purchase flow can only be finished on a Play test track, so it also
waits for BL-005. M5's
owner tasks (account, payments) can start at any time and are on the critical
path, because M6 takes at least 14 days.

## Stop Conditions

- Harness not accepted.
- Product goal unclear.
- A validation gate cannot be satisfied.
- Agent capability downgrade blocks required review or verification.
- A Play policy requirement cannot be met without changing a decision in an ADR.

## Rules

- Milestones describe intended outcomes, not progress. Status lives in
  `agents/RUN_STATE.md`.
