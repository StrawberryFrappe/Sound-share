# Backlog

A registry of what could be done and why. **It carries no status.** Status lives
in `agents/RUN_STATE.md` only. No external board is used.

| ID | Area | Item | Why / Source | Done Evidence |
|---|---|---|---|---|
| BL-001 | Harness | Complete harness mounting | Owner request, 2026-11-16 | Owner accepts the mount review in `agents/reviews/20261116/` |
| BL-002 | Verify | Real-device check of 0.3.0 on the owner's tablet and phone: QR pairing, sound steadiness ("% lost" and dropouts), lip sync at the default delay | GATE-DEVICE; ASM-007, ASM-008 | Owner's report recorded in `agents/validation/EVIDENCE_INDEX.md` |
| BL-003 | Feature | Research spike: private network pairing (local-only hotspot vs Wi-Fi Direct), including whether the tablet keeps its own Wi-Fi | ADR 0007 | Follow-up ADR with the chosen approach, and test results from the owner's devices |
| BL-004 | Design | Decide the visual direction (polish the current Material look, or a distinct brand), then a polish pass: icon, spacing, empty and error states, store screenshots | Owner goal "stylistic changes"; direction open | Owner approves real-device screenshots; GATE-PQ |
| BL-005 | Store | Owner creates the Google Play developer account (fee, identity verification) and a payments profile, needed to sell in-app products | ADR 0008; owner has no account yet | Account exists; noted by the owner |
| BL-006 | Store | Closed test with at least 12 testers for 14 days, which new personal developer accounts need before production; the listener stakeholder is one tester | Play policy (SRC-006) | Play Console shows the requirement met |
| BL-007 | Store | Store listing in Spanish and English: description, screenshots, icon, feature graphic. States plainly that listening is free and hosting is a one-time 500 CLP purchase with a free trial, that both devices need the app, that apps like Netflix cannot be shared, and that both devices must be on the same network (or use the private network, once it exists) | ADR 0008; Non-Goals in `agents/intake/PROJECT_BRIEF.md` | Listing text reviewed by the owner |
| BL-008 | Store | Publish `PRIVACY.md` at a stable public URL for the listing | Play requires a privacy policy for the microphone permission (SRC-006) | URL entered in the Play Console |
| BL-009 | Store | Data safety form; permission and foreground-service declarations (media projection, media playback) with any video the Console asks for | SRC-006 | Declarations accepted by the Console |
| BL-010 | Repo | Move release work to `main` and stop using the `claude/` branch prefix | ADR 0003 known conflict | `main` holds the release commit; store builds come from it |
| BL-011 | Store | Enroll in Play App Signing with the existing release key as the upload key; the owner keeps a backup of the key file and its passwords | ASM-002 | Play Console shows the upload key fingerprint `8e:c9:54:a5:...` |
| BL-012 | Feature | Tell the sharing user when the playing app blocks capture (media is playing but the captured sound is pure silence for a few seconds), so they are not left guessing | Netflix confusion, 2026-11-14; fewer refunds, especially during the trial (ADR 0008) | Test on a device with a capture-blocking app; owner sees the message |
| BL-013 | Audio | Compress the stream (Opus) if real-device loss stays high on busy Wi-Fi | ADR 0005; BL-002 results | Lower "% lost" on the owner's devices at the same delay |
| BL-014 | Quality | Continuous integration that runs unit tests and lint on every push | GATE-TEST is manual today | Green workflow run on GitHub |
| BL-016 | Feature | Hosting unlock: Google Play Billing for a one-time 500 CLP product; an "unlocked" record trusted offline; re-check, restore and refund handling when online; a purchase screen in English and Spanish | ADR 0008 | Unit tests for the unlock logic; on a test track with a license tester: buy, go offline, restart, still unlocked; reinstall restores; refund removes |
| BL-017 | Feature | Free hosting trial (length per the open question in `agents/intake/QUESTIONS_SUMMARY.md`), counted on the device | ADR 0008 | Unit tests; owner tries the trial running out on a real device |
| BL-018 | Store | Update `PRIVACY.md` and the Data safety form for in-app purchases: Google Play handles payment, the app keeps only an "unlocked" flag and the trial time on the device | ADR 0008; SRC-005 | Updated policy published; Data safety form accepted |
| BL-015 | Quality | A check that every English app string has a Spanish translation and the reverse | GATE-L10N | Check fails when a string is missing, passes now |

## Rules

- Backlog items are tied to source, ADRs, owner decisions, or explicit project
  goals.
- Do not pad the backlog with work that has no product, quality, evidence, or
  operational value.
- If a work item changes visible behaviour, define how it will be verified.
- Remove an entry once it is done and its evidence is recorded elsewhere.
