# Questions Summary

Accepted answers in digestible form. The raw answers stay in `agents/local/`.

## Answered Decisions

| Date | Topic | Accepted Answer | Promoted To |
|---|---|---|---|
| 2026-11-13 | Product | Share one device's playing audio to another device's headphones over Wi-Fi; same app on both | `agents/intake/PROJECT_BRIEF.md` |
| 2026-11-14 | DRM apps | Netflix blocks capture; the app does not try to get around it | `agents/intake/PROJECT_BRIEF.md` (Non-Goals) |
| 2026-11-15 | Goal | Sell on Google Play for 500 CLP; needs stylistic changes, security, and a hotspot feature | `agents/planning/ROADMAP.md` |
| 2026-11-16 | Contributors | Just the owner, with one stakeholder in mind | ADR 0002 |
| 2026-11-16 | Stakeholder | The person listening (phone with wired headphones) | `agents/intake/PROJECT_BRIEF.md` |
| 2026-11-16 | AI attribution | None from now on; the commit hook is enabled. Earlier commits keep their trailers | ADR 0003 |
| 2026-11-16 | Pricing (first answer) | Paid app, 500 CLP; no in-app purchases | ADR 0004, superseded |
| 2026-11-16 | Pricing (owner's revision) | Free app; listening free; hosting unlocked by a one-time 500 CLP in-app purchase that keeps working offline; a short free hosting trial | ADR 0008 |
| 2026-11-16 | Harness language | English | ADR 0002 |
| 2026-11-16 | Play developer account | Not yet created | `agents/planning/BACKLOG.md` BL-005 |
| 2026-11-16 | Hotspot feature | Before launch | ADR 0007 |
| 2026-11-16 | Visual style | Not sure yet (proposed default: decide after seeing real-device screenshots) | `agents/planning/BACKLOG.md` BL-004 |

## Unanswered Questions

| Priority | Question | Why It Matters | Default If Deferred |
|---|---|---|---|
| P1 | Is the current stack (Kotlin, Compose, Android 6+ for listening, Android 10+ for sharing) accepted as the long-term stack? | Changing it later is expensive | Treat as accepted once the harness is accepted (ADR 0005, proposed) |
| P1 | Is `io.github.strawberryfrappe.soundshare` the final application id, and "Sound Share" the final store name? | The id can never change after publishing; the name may collide with other listings | Keep both; check the store for name clashes before the listing |
| P1 | Which tablet model is the sharing device? | Decides whether the hotspot can run while the tablet stays on its own Wi-Fi (ADR 0007) | Research both the hotspot and Wi-Fi Direct approaches |
| P1 | Development so far is on `claude/android-audio-streaming-xmkci1`, and commits are authored as the agent environment's identity. The no-attribution rule forbids both. Move to `main`, and commit under the owner's identity? | Rule conflict, and the store build should come from a clean branch | Keep both for now; move to `main` before the first store upload (BL-010) and switch identity when the owner provides one (BL-019) |
| P1 | Confirm the proposed unlock design in ADR 0008: Google Play Billing, an unlock record trusted offline indefinitely, re-check online, no server | The owner set the goal (works offline); the mechanism is the agent's proposal | Treat as confirmed once the harness is accepted |
| P2 | What does the stakeholder (the listener) need to see before launch: a demo, a test build? | Decides when their feedback enters | Give them the closed-test build (BL-006) |
| P2 | Should a requirements spec (SRS) exist? | The kit offers one; the project is small | No SRS; ADRs and the backlog carry intent |
| P2 | Model and usage budget for subagents | The workflow assigns model tiers; cost is the owner's | Follow the workflow's tier table; keep fan-out small |
| P2 | Should Spanish be the store listing's default language? | First market is Chile | Spanish default, English secondary |
| P1 | How long, and what kind, is the free hosting trial (minutes in total, minutes per session, a number of sessions)? | Too short and buyers cannot check their apps; per-session limits can be restarted forever | 30 minutes of hosting in total, then the unlock is required. BL-017 is not implemented until the owner answers |
| P3 | Is 500 CLP an allowed in-app product price in Chile? | The price must be allowed | Check in the Play Console when the account exists |

## Grill Checklist

- [x] Product goal and audience
- [x] Existing code maturity
- [x] Source authority
- [ ] Stack and architecture (stack inferred, confirmation pending)
- [x] Data and persistence (local only; see `PRIVACY.md`)
- [x] Authentication and security (pairing code, ADR 0006)
- [x] Deployment target (Google Play)
- [x] Test and evidence bar
- [ ] User-facing quality expectations (style direction open)
- [x] Collaboration and handoff preferences
