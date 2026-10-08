# Mobile design decisions

Platform: Fire OS 7/8 (Android 9+, API 28+), with Android tablets and phones supported.
Framework: Kotlin, Jetpack Compose Material 3. No Google Play Services dependency.
Audience: consumer Fire tablet owners organizing books, downloads and removable media.
Offline: all file operations are local; no server, analytics, account, or network permission.

Navigation: one folder stack, a floating places panel at 840dp and wider, and a glass navigation bar below the content on compact windows. Tools and favorites are visible destinations. Android Back walks the folder stack. Dialogs use system Back and visible cancellation.

Mobile checkpoint: android-dev and mobile-design guidance read, including design thinking, touch psychology, Android platform, performance, backend, testing and debugging references.
Principles: 48dp targets; file I/O on IO dispatchers; explicit progress, failure and retry states. Avoid gesture-only actions and loading entire media into memory.
MFRI: platform 5 + accessibility readiness 4 - interaction 2 - performance 3 - offline dependence 1 = 3. Requires emulator UX validation and real Fire/USB testing before release.

Visual direction: blue-gray light fields, cream glass panels, soft yellow folder accents, and large Material 3 Expressive headings. The supplied visual reference informed the palette, floating navigation and layered shapes; its photographs, logo and UI assets are not included. Folder illustrations are composed from original panels and Apache-licensed Material icons. Fonts are supplied by Android. Light and dark themes use separate contrast palettes.

Glass uses high-opacity translucent fills, gradient light reflections, a specular border and 2dp elevation. The background is a cached static drawing. This works on API 28 without a backdrop-blur library, bitmap capture, shader refraction or continuous animation. Text remains sharp over the tinted surfaces. This is frosted glass styling; it does not claim to reproduce Apple's Liquid Glass rendering.

Motion: MaterialExpressiveTheme with MotionScheme.expressive(), color/page transitions lasting 180 to 220ms, spring press feedback and button shape changes, favorite emphasis, and an expanding selection toolbar. Animations affect drawing rather than list layout wherever possible. Android's animation scale controls the Compose runtime; custom effects also disable transforms when ValueAnimator reports disabled animations, including changes while the app is open. No idle animation loop is used.

Interaction: tap opens an item, long press selects it, and a visible checkbox offers the same selection action. Overflow menus retain file operations and destructive confirmations. Touch targets remain at least 48dp. Compact destination labels can wrap for enlarged fonts, and scrolling content reserves space for the bottom bar.

Dependency choice: the Expressive theme and motion APIs are in the Material 3 prerelease line. Material 3 1.5.0-alpha09 is pinned explicitly with stable Compose BOM 2025.11.01. Review upstream API changes and repeat API 28 UI checks before upgrading this dependency.

Palette checks cover ten text/surface combinations, including secondary text over the least opaque glass fill and darkest backdrop endpoint. Calculated ratios range from 6.70:1 to 13.91:1. These checks cover the chosen solid colors and alpha composites; they are not a complete accessibility audit. Native controls retain their labels, roles and focus behavior.

Screen decomposition:
- Places: connect a folder or pick downloads; large visible buttons, scrollable persistent roots and favorites. Permission failures retain a reconnect action.
- Browser: primary action is opening a row, with accessible overflow actions and checkboxes. Lazy list keyed by document URI. Current-folder search is explicit; sort, hidden files, details, refresh and breadcrumb Back are visible.
- Tools: archive creation/extraction and SHA-256 duplicate review are explicit actions. Duplicates are never automatically deleted.
- Transfers: choose destination using the system folder picker; show progress and cancellation, keep source on failed verification. Keep app open; durable background jobs are a later milestone.
- Preview: sampled bitmap, native VideoView with playback controls, external PDF/EPUB reader via URI grant.

Real hardware validation remains required for storage-provider quirks, USB removal, low-memory behavior, TalkBack and Fire launcher widget support.
