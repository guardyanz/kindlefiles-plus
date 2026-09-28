# KindleFiles+

A local file manager for Fire tablets. Kotlin + Jetpack Compose, built independently using Android's Storage Access Framework (SAF) and AndroidX DocumentFile.

**Status: 0.1.0 development MVP.** This is a working starting point, not an Amazon Appstore release. No backend, accounts, ads, analytics, Google Play Services or internet permission.

## What works

- Connect user-selected folders on internal storage, SD cards and USB volumes exposed by the system picker; persist folder access.
- Browse, current-folder search, name/date/size sorting, hidden-file toggle, metadata, folder creation and rename.
- Multi-select files; copy or move between selected folders. SHA-256 read-back verifies the copy before a move deletes its source. Name collisions are refused.
- Pick individual downloads when Android will not grant access to the entire Downloads directory.
- Create ZIP archives from selected files; extract nested ZIP archives into a new folder. Traversal paths, duplicate entries, excessive nesting, more than 10,000 entries and expansion over 2 GiB are rejected.
- Scan a folder and subfolders for SHA-256 duplicates; review and confirm each deletion. Nothing is deleted automatically.
- Persist folder favorites. Sampled image preview and native video playback. Open PDF/EPUB with a compatible installed reader.
- Share files using Android's chooser. Choose Kindle if the installed Kindle app offers a compatible share target; no private Amazon API or automatic library upload.
- Configurable folder widget on launchers that support Android widgets.
- Adaptive tablet sidebar, compact navigation, light/dark palettes, original launcher vector and Apache-licensed Material icons.

## Build

Requirements: JDK 17 or newer (tested with Android Studio JBR 21), Android SDK platform 36 and build tools 35, and an SDK path in `local.properties` or `ANDROID_HOME`.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Android/Android Studio/jbr'
.\gradlew.bat :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Install using `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
Release builds use `:app:assembleRelease` and are unsigned until you configure your own signing process. Never commit signing keys.

Pinned baseline: AGP 8.13.2, Gradle 8.13, Kotlin 2.2.10, Compose BOM 2024.09.00, AndroidX DocumentFile 1.1.0. Minimum API 28 (Fire OS 7/Android 9); target API 35. Review Amazon's requirements again before submission.

## Storage behavior and limits

Access is scoped to what the user grants through SAF. This app cannot browse every path on a tablet. Android 11 restricts root volumes, the Downloads tree, `Android/data` and `Android/obb`. Use **Pick downloads / files** for individual downloads. A USB drive must be mounted and exposed by the device's document provider; there is no raw USB driver.

Transfer and ZIP creation currently operate on selected **files**. Folder copy/move and folder ZIP creation are scheduled next; extraction supports folders. Encrypted, split and ZIP64 archives are unsupported. Search filters the current folder; duplicate scans recurse, with a 10,000-item/32-level bound. Some providers report unknown sizes or stale metadata; real Fire hardware testing remains necessary.

Keep the app open during a long operation. Rotation is supported by the ViewModel, but jobs do not resume after process death, force stop, or power loss. Cooperative cancellation removes the current partial output when the provider permits it; completed batch items remain completed. After abrupt process death, inspect the destination for partial output before retrying. Provider failures are shown; no silent overwrites. As with other provider-based operations, a concurrent edit in another app cannot be made atomic with source deletion.

Disconnect removes a saved place, retaining grants potentially used by favorites or widgets. Revoke all app access by clearing its data or uninstalling. Deletion is permanent and requires confirmation.

## Project map

```text
app/   Compose UI, ViewModel, SAF adapter, favorites and folder widget
core/  Provider-neutral streaming file engine and safety regression tests
docs/  Design, release plan, device test matrix, privacy and licensing notes
```

See [design decisions](docs/DESIGN.md), [release roadmap](docs/ROADMAP.md), [test results and device matrix](docs/TESTING.md), [privacy](docs/PRIVACY.md) and [third-party notices](NOTICE).

## Licensing and provenance

New code and original assets: **Apache-2.0**, copyright 2026 guardyanz. See [LICENSE](LICENSE). Amaze and Material Files are UX references only; their source, assets, screenshots and GPL components were not copied or forked. AndroidX, Kotlin and Material icons supply the implementation foundation. See [dependency notes](docs/DEPENDENCIES.md).

KindleFiles+ is an independent project, not affiliated with Amazon. Kindle and Fire are Amazon trademarks. Branding and store listing review are part of the pre-release work.

## Primary platform references

- [Android Storage Access Framework and access restrictions](https://developer.android.com/training/data-storage/shared/documents-files)
- [AndroidX DocumentFile](https://developer.android.com/jetpack/androidx/releases/documentfile)
- [Fire OS 8 for Fire tablets](https://developer.amazon.com/docs/fire-tablets/fire-os-8.html)
- [Amazon Send to Kindle](https://www.amazon.com/sendtokindle)
- [AGP 8.13 release notes and compatibility](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
