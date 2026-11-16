# ADR 0008: Free Listening, One-Time Unlock for Hosting

## Status

Proposed on 2026-11-16; accepted when the owner accepts the mounted harness.
Supersedes ADR 0004.

## Context

ADR 0004 made Sound Share a plain paid app, so a watcher and a listener on
different Google accounts would both pay. The owner proposed instead: the free
version only listens, the paid version also hosts (shares). Two ways were
compared on 2026-11-16: one free app with an in-app unlock, or two apps (a free
"Lite" listener and a paid "Pro"). The owner's main worry was the app stopping
working without internet.

## Decision

Decided by the owner on 2026-11-16:

- **One free app** on Google Play. Listening is always free.
- **Hosting is unlocked by a one-time in-app purchase of 500 CLP.** No
  subscription, no ads, no accounts.
- **A short free hosting trial**, so a buyer can check that their video app
  allows its sound to be shared before paying. Its length and form are an open
  question (`agents/intake/QUESTIONS_SUMMARY.md`).
- **Hosting keeps working offline after purchase.** This was the owner's
  condition for choosing the unlock over a separate paid app.

## Proposed Design

Proposed by the agent to meet the owner's condition; the owner confirms or
corrects it when accepting the harness.

- A non-consumable product bought through Google Play Billing.
- The app records "hosting unlocked" on the device when Google Play confirms
  the purchase, and trusts that record without a connection, indefinitely.
  When online, it re-checks with Google Play, which also restores the unlock
  after a reinstall and removes it after a refund. Streaming itself never needs
  the internet.
- Purchase checks happen on the device only; there is no server. A rooted
  device could fake the unlock (ASM-010).

## Consequences

- The listener never pays, which removes ADR 0004's two-purchase problem.
- One store listing, labelled "Contains in-app purchases". The listing and the
  app state plainly: listening free, hosting a one-time 500 CLP purchase.
- New code: Google Play Billing, the unlock record, the trial, and the
  purchase screen. The purchase flow can only be tested once the Play developer
  account exists and the app is on a test track (BL-005, BL-016).
- `PRIVACY.md` and the Data safety form must mention that Google Play handles
  the payment and the app keeps only an "unlocked" flag on the device (BL-018).
- The unlock flag must be stored where a backup restore cannot copy it to
  another account's device. App data is already excluded from backups
  (ADR 0006); after a reinstall the unlock comes back from Google Play, which
  needs a connection once.
- Devices without Google Play cannot buy the unlock; they can still listen.

## Sources

- SRC-001 (owner proposal and choice, 2026-11-16)
