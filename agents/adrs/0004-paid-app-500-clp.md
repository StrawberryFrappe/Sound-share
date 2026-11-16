# ADR 0004: Sell as a Paid App at 500 CLP

## Status

Proposed on 2026-11-16; accepted when the owner accepts the mounted harness.

## Context

The owner wants to sell Sound Share on Google Play for 500 CLP. Both devices
need the app. Options considered: a paid app; a free app with a one-time
in-app purchase that unlocks sharing (listening free); deciding later.

## Decision

Publish as a **paid app at 500 CLP**, with no in-app purchases, ads or
accounts.

## Consequences

- No billing code; the app keeps collecting no data (`PRIVACY.md`).
- One purchase covers every device signed in to the same Google account. When
  the watcher and the listener use different accounts, both buy the app. The
  store listing must say so plainly (BL-007).
- Requires a Play developer account with a payments profile (BL-005).
- Paid apps get refund requests; honest limits in the listing (no Netflix,
  both devices need the app, same Wi-Fi) are the main defence.
- If the two-purchase problem hurts sales, revisit with the free-plus-unlock
  model. That would need Play Billing and a new privacy review.

## Sources

- SRC-001 (owner answer, 2026-11-16)
