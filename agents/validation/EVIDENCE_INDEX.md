# Evidence Index

| ID | Date | Evidence | Proves | Location |
|---|---|---|---|---|
| EV-001 | 2026-11-15 | 34 unit tests pass for 0.3.0, including the network attack cases | Pairing, encryption and replay protection behave as designed (ADR 0006) | `app/src/test/`; rerun with `./gradlew testDebugUnitTest` |
| EV-002 | 2026-11-15 (historical claim) | Release APK 0.3.0 on an Android 11 emulator without Play services: sharing started; scanner fell back to typing; a mistyped code was rejected; the correct code connected and played | Release build works end to end without the scanner (GATE-RELEASE) | Agent session notes, `agents/record/SF-20261116/history.md` |
| EV-003 | 2026-11-16 | English and Spanish string sets have the same names (re-run at the mount: 58 names each, none missing either way) | GATE-L10N for 0.3.0 | Compare the `name=` attributes of `app/src/main/res/values/strings.xml` and `app/src/main/res/values-es/strings.xml`, excluding the untranslatable `app_name` |
| EV-004 | 2026-11-15 (historical claim) | AES-GCM seal plus open costs 1.8 µs per 5 ms packet on a desktop JVM | Encryption cost is negligible (ASM-008) | `agents/record/SF-20261116/history.md` |
| EV-005 | 2026-11-13 | Owner installed the 0.1.0 release on real devices (after removing the earlier debug build); installation worked; the connection was "kinda iffy sometimes" | Core flow works on hardware; stability needed work (led to 0.2.0 changes) | Owner's message, summarised in `agents/record/SF-20261116/history.md` |
| EV-007 | 2026-11-16 | Owner: "I accept the setup" | GATE-HM passed | Owner's message, summarised in `agents/record/SF-20261116/harness-mount.md` |
| EV-006 | 2026-11-14 | Owner reported Netflix does not work when sharing | Capture-blocking apps are a real buyer issue (BL-012, BL-007) | `agents/record/SF-20261116/history.md` |

## Rules

- Evidence should be easy to inspect when it matters to the owner.
- Screenshots or the owner's visual review are required when user-facing
  quality matters.
- Append rows; do not rewrite old ones.
