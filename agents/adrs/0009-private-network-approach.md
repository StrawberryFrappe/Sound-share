# ADR 0009: Private Network Approach (Proposal)

## Status

Proposed on 2026-11-16; needs the owner's answers and a test on the owner's
tablet before it can be accepted. Follows ADR 0007.

## Context

ADR 0007 put a private network (no shared Wi-Fi needed) in scope before
launch. Desk research is in `agents/record/SF-20261116/hotspot-research.md`.
The sharing device must keep its own Wi-Fi connection, because it is usually
streaming the video it shares, and whether that works is device-specific.

## Proposal

1. **Sharing device: Wi-Fi Direct group** (Android 10+, which sharing already
   needs). The app creates a group with a name and password it chooses, so the
   pairing QR code can carry them. If group creation fails, or the device's own
   Wi-Fi drops when it starts, fall back to a local-only hotspot only when the
   device reports STA+AP concurrency; otherwise tell the user to use a shared
   network.
2. **Listening phone, Android 10+:** one scan joins the network through the
   system's connection dialog, then pairs as today. The app binds its sockets
   to that network.
3. **Listening phone, Android 6 to 9:** the app shows the network name and
   password to join in Settings, then pairs as today.
4. **First, a probe build** for the owner: a "private network" test on the
   share screen that creates the group and reports whether the device's own
   Wi-Fi stayed connected and video kept playing. Run on the owner's tablet
   before building the full flow.

## Costs To Decide

- **New permission on the sharing device:** "nearby Wi-Fi devices" on
  Android 13+, and **location** on Android 10 to 12. The location prompt is
  unexpected in an audio app and must be explained on screen, in `PRIVACY.md`
  and in the Play declarations; the app would not use location.
- The private network gives the listening phone no internet while it is joined.
- Behaviour varies by device; the fallback keeps the shared-network flow.

## Questions For The Owner

- Which tablet model, and which Android version, is the sharing device?
- Is a location permission prompt on Android 10 to 12 tablets acceptable for
  this feature?

## Sources

- `agents/record/SF-20261116/hotspot-research.md` (Android documentation)
- ADR 0007
