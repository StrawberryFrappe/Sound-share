# ADR 0007: Private Network (Hotspot) Pairing Before Launch

## Status

Accepted by the owner on 2026-11-16. The scope is decided; the technical approach is not.

## Context

Today both devices must share a Wi-Fi network. The owner wants a hotspot-based
flow to streamline setup, and chose to have it **before launch**.

Constraints found so far (to be verified against current Android documentation,
SRC-007):

- Apps cannot turn on the regular tethering hotspot. They can start a
  *local-only hotspot* or a *Wi-Fi Direct group*.
- If the tablet loses its own Wi-Fi when it starts a network, the video it is
  streaming stops. So the network must coexist with the tablet's Wi-Fi
  connection, and support for that varies by device.
- The listening phone can join a network the app names, through the system's
  network-request dialog, without leaving the user's settings.
- The pairing QR code could carry the network credentials as well as the
  pairing code, so one scan does everything.

## Decision

- The private network flow is in scope for the first paid version.
- First a research spike (BL-003) decides between local-only hotspot and
  Wi-Fi Direct, by checking API behaviour and testing on the owner's tablet and
  phone. The result becomes a follow-up ADR.
- The existing same-network flow stays as the fallback.

## Consequences

- Launch depends on this feature; if the spike finds no reliable approach on
  common devices, the owner must decide whether to launch without it.
- New permissions may be needed (nearby Wi-Fi devices or location on some
  Android versions). Each one must be justified in the store declarations and
  reflected in `PRIVACY.md`.

## Sources

- SRC-001 (owner answers, 2026-11-15 and 2026-11-16)
