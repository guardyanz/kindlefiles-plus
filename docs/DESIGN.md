# Mobile design decisions

Platform: Fire OS 7/8 (Android 9+, API 28+), with Android tablets and phones supported.
Framework: Kotlin, Jetpack Compose Material 3. No Google Play Services dependency.
Audience: consumer Fire tablet owners organizing books, downloads and removable media.
Offline: all file operations are local; no server, analytics, account, or network permission.

Navigation: one folder stack, a places panel on wide windows, and a places button on compact windows. Tools and favorites are visible destinations; no five-tab dashboard. Android Back walks the folder stack. Dialogs use system Back and visible cancellation.

Mobile checkpoint: android-dev and mobile-design guidance read, including design thinking, touch psychology, Android platform, performance, backend, testing and debugging references.
Principles: 48dp targets; file I/O on IO dispatchers; explicit progress, failure and retry states. Avoid gesture-only actions and loading entire media into memory.
MFRI: platform 5 + accessibility readiness 4 - interaction 2 - performance 3 - offline dependence 1 = 3. Requires emulator UX validation and real Fire/USB testing before release.

Visual direction: warm paper surfaces, forest green actions, amber folder accents, generous readable rows. Original layout and vector launcher asset. System fonts and Apache-licensed Material icons. Light and dark themes.

Screen decomposition:
- Places: connect a folder or pick downloads; large visible buttons, scrollable persistent roots and favorites. Permission failures retain a reconnect action.
- Browser: primary action is opening a row, with accessible overflow actions and checkboxes. Lazy list keyed by document URI. Current-folder search is explicit; sort, hidden files, details, refresh and breadcrumb Back are visible.
- Tools: archive creation/extraction and SHA-256 duplicate review are explicit actions. Duplicates are never automatically deleted.
- Transfers: choose destination using the system folder picker; show progress and cancellation, keep source on failed verification. Keep app open; durable background jobs are a later milestone.
- Preview: sampled bitmap, native VideoView with playback controls, external PDF/EPUB reader via URI grant.

Real hardware validation remains required for storage-provider quirks, USB removal, low-memory behavior, TalkBack and Fire launcher widget support.
