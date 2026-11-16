# 0.3.1 batch

- BL-012: `core/SilenceWatch.kt`. Pure-zero capture for a while, with
  `AudioManager.isMusicActive()` true, shows a warning card while sharing.
  First cut used 5 s and said the app "does not allow" sharing; the code review
  (a Sonnet subagent) flagged false positives from muted players and silent
  stretches, so it is 8 s and says "may not allow". Also from the review: the
  capture thread no longer writes state after stopping; the card is a polite
  live region for screen readers.
- BL-015: `StringResourcesTest` checks names, placeholders, and that
  untranslatable texts are not translated.
- BL-014: `.github/workflows/ci.yml` installs SDK platform 37 and build-tools,
  then runs unit tests, lint and a release build (debug-key fallback). Review
  asked for the SDK step and the release build; both added.
- Release smoke on an Android 11 emulator passed (EV-008). The warning itself
  could not be triggered there (no media apps on the image); it needs the
  owner's tablet with a capture-blocking app, as BL-012 says.
- Version 0.3.1 (code 4), commit `c7c1e7f`.
- CI's first two runs passed on GitHub (runs 43785310898, 43785409498), so the
  SDK install step and the debug-key fallback for release builds work on a
  fresh runner. BL-014 removed from the backlog (EV-010).
