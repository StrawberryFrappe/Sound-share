# Assumptions

Filled in from repository evidence and earlier conversation rather than asked
directly. The owner accepted the harness with these visible on 2026-11-16; they
stay listed until proven or disproven. Retired: ASM-001 (stack, confirmed in
ADR 0005) and ASM-010 (on-device purchase checks, confirmed in ADR 0008). The
application id and the name "Sound Share" were accepted as final.

| ID | Assumption | Risk | Expiration / Review Trigger |
|---|---|---|---|
| ASM-002 | The release signing key generated on 2026-11-13 is the upload key for Google Play, and the owner keeps the copy that was sent to them. | High: a lost key blocks updates under the same listing | Confirm when enrolling in Play App Signing (BL-011) |
| ASM-003 | Store launch is Chile first, with Spanish as the main language. | Low | Owner decides the listing (BL-007) |
| ASM-004 | Up to eight listeners per sharing device is enough. | Low | Owner feedback or reviews |
| ASM-005 | 72-bit pairing secrets with AES-128-GCM are strong enough for the threat model: strangers and curious people on the same Wi-Fi, not well-resourced attackers. Known gaps accepted under it: the hello's source address is not authenticated, so someone who can sniff the network could replay a fresh hello from a spoofed address and point the stream elsewhere for up to about 5 s at a time; the audio sequence number is 32-bit, so nonces stay unique only for sessions shorter than about 248 days. | Medium | Any security review, or a change of audience |
| ASM-006 | The record slug for the owner is `SF`, from the GitHub handle StrawberryFrappe. | Low | Owner prefers another slug |
| ASM-007 | The default listening delay of 120 ms suits most setups, including a tablet that also plays to Bluetooth headphones. | Medium: perceived sync is the listener's main concern | Owner's real-device report (BL-002) |
| ASM-008 | The 27 % late-audio figure seen for 0.3.0 in an unaccelerated emulator comes from the emulator simulating AES instructions in software, not from the app. AES-GCM measured 1.8 µs per 5 ms packet on a desktop JVM (EV-004). | Medium | Owner's real-device "% lost" figure (BL-002) |
| ASM-009 | Google Play services is present on buyers' devices, so the QR scanner works; without it the user types the code. | Low | Reports from devices without Play services |

## Rules

- Do not let assumptions silently become requirements.
- High-impact assumptions need ADRs or owner confirmation.
- If an owner correction invalidates an assumption, update affected planning,
  architecture, validation, and traceability docs.
