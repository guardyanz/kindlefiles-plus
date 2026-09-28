# Release plan

Android first; Windows deferred to v2. Candidate business model: paid app at USD 1.99, subject to Appstore pricing setup. No billing SDK or paywall is included in this development build.

## Week 1 — foundation (implemented in 0.1.0)

- Independent Apache-2.0 repo, Kotlin/Compose, SAF adapter, streaming engine.
- Folder browser, search, favorites, file transfer, archive operations, duplicate review, previews, sharing, widget.
- JVM regression tests, Android lint/build CI, documented storage limits.

## Week 2 — storage reliability

- Physical Fire OS 7/8 tests: internal folders, SD, USB OTG; canceled/revoked permissions, removed drives, read-only volumes and low disk space.
- Durable WorkManager operation journal with explicit interrupted-job recovery before claiming background transfers.
- Recursive folder transfer/ZIP creation with destination ancestry checks, collision strategy and source snapshot validation.
- Provider-aware capability flags; large directory and unknown-size optimization.

## Week 3 — UX and compatibility

- TalkBack and 200% font scaling, low-memory tablets, media formats and rotation.
- System picker differences on Fire firmware; launcher widget compatibility.
- Better multi-file share, recursive search, remembered sort/filter preferences, Indonesian localization.
- Fire OS 8/API 30 instrumentation and lowest-supported API 28 regression coverage.

## Weeks 4–5 — commercial release

- Review branding, original artwork, release dependency/license inventory and Amazon policies.
- Add publisher contact, host privacy policy, release signing and key recovery documentation.
- Decide paid listing versus Amazon IAP themes/tabs; price configuration outside the codebase.
- Internal testers, accessibility/device acceptance, signed release APK and store screenshots.
- No antivirus claims, app-store updater claims, Play Store bypass, or undocumented Amazon library APIs.

## Windows v2

Reassess a narrow WinUI shell for ZIP, hashes and duplicate review. Port the provider-neutral engine contract or extract a platform library only after Android demand is validated. Estimated two additional weeks is a planning assumption, not a delivery promise.
