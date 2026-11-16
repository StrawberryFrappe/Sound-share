# ADR 0006: Pairing Code and Encrypted Stream

## Status

Implemented in 0.3.0 (commit `a9cefe7`) before the harness existed. Accepted by the owner on 2026-11-16.

## Context

Up to 0.2.0, anyone on the same Wi-Fi could listen: the sharing device streamed
in the clear to any address that said hello. A spoofed hello could also aim
1.5 Mbit/s at a third party. A paid app cannot ship that way.

## Decision

- The sharing device holds a pairing secret of 72 random bits, shown as a QR
  code and as 16 characters (Crockford base32 with an 8-bit checksum). It
  persists until the user makes a new one, which unpairs everyone.
- Each sharing session derives a fresh AES-128-GCM key from the secret and a
  random 16-byte session nonce (HMAC-SHA256 as the key derivation function).
- Audio, the listeners' hellos and goodbyes, and the sender's goodbye are
  encrypted or authenticated. Hellos carry a counter; replays are refused even
  after the listener leaves. GCM nonces never repeat under one key.
- Discovery beacons stay in the clear and carry only the device name, an
  identifier of the code and the session nonce. Probes are padded so a reply
  is never larger than the request.
- Packets from the unencrypted protocol (version 1) are ignored.
- App data is excluded from backups and device transfer, so the secret never
  leaves the device that made it.

Design and wire format: `app/src/main/java/io/github/strawberryfrappe/soundshare/core/Protocol.kt`.
Tests: `app/src/test/java/io/github/strawberryfrappe/soundshare/core/`.

## Consequences

- Strangers on the network cannot listen, inject sound, or redirect the stream
  (covered by `StreamLoopbackTest`). One limit: the hello's source address is
  not authenticated, so someone who can sniff the network could replay a fresh
  hello from a spoofed address and redirect the stream for up to about 5 s at a
  time (ASM-005).
- The audio sequence number is 32-bit, so nonces stay unique only for a
  single session shorter than about 248 days of continuous streaming.
- Not a password-authenticated key exchange: someone who captures the traffic
  **and** already knows the code could decrypt it. Acceptable for this threat
  model (ASM-005).
- Device names travel in the clear on the local network (`PRIVACY.md` says so).
- 0.3.0 cannot talk to 0.2.0; both devices must update together.

## Sources

- SRC-001 (owner asked for security, 2026-11-15), SRC-003 (code and tests)
