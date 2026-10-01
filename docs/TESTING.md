# Verification

## Automated checks

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

The core suite exercises verified file/folder moves, nested and empty folder archives, descendant destination refusal, corrupt providers, source-delete failure, source mutation, cancellation rollback, existing-file collisions, Unicode/empty ZIP round trips, nested archive paths, traversal/absolute/drive/backslash paths, case collisions, expansion and entry limits, truncated ZIP footers, empty archives, SHA-256 vectors, bounded walking and hidden-file handling.

Android JVM tests use Robolectric with API 28 and the real Activity/Compose UI to check first launch, navigation, tool preconditions and the About dialog. These do not replace a Fire storage-provider or launcher test.

## Local environment limitation

The installed Windows Android emulator has no working hardware-acceleration driver. A local headless software-mode attempt did not reach an online ADB device and was stopped. Screenshot capture instead uses a hardware-accelerated Linux emulator in GitHub Actions. The publisher confirmed a physical Fire tablet is not yet available; the physical acceptance matrix remains pending.

## Emulator screenshot capture

The [Tablet screenshots workflow](../.github/workflows/screenshots.yml) downloads an existing signed private release APK and captures its actual UI using ADB. The [successful capture run](https://github.com/guardyanz/kindlefiles-plus/actions/runs/36827704863) used AOSP API 28, x86_64, Nexus 10 profile, 2560×1600 landscape, and 320 dpi. [Store PNGs and provenance](../store-assets/screenshots/README.md) are committed separately from temporary UI dumps and sample documents under ignored `artifacts/`.

The capture journey launches the signed app, grants access to a disposable Downloads subfolder through the real system picker, browses folders, saves favorites, opens the ZIP actions menu, and scans an identical pair of documents. This confirms those UI paths on AOSP; it does not exercise transfers, archive extraction, USB/SD hardware, Fire OS providers, or Fire launchers.

To repeat against an uploaded private release:

```sh
gh workflow run screenshots.yml -f release_tag=v0.9.0-rc1
```

## Device acceptance matrix (pending)

| Area | Required checks |
| --- | --- |
| Devices | Fire OS 7/API 28 and Fire OS 8/API 30; Android API 35/36 |
| Grants | Picker cancel, read-only access, reboot persistence, grant revoke, folder renamed externally |
| Downloads | Pick PDF/EPUB files individually and copy to a selected destination |
| SD/USB | Copy/move, unplug during write and verification, reconnect, read-only medium, full disk |
| Files | Unicode names, collisions, zero-byte files, 1+ GiB transfer, source changed by another app |
| ZIP | Valid nested/empty archive, malformed/truncated/encrypted/ZIP64 archive, expansion limit, cancellation |
| Duplicates | Same content/different name, same size/different content, hidden folders, manual deletion |
| Lifecycle | Rotation during operation, background/foreground, process death; inspect partial destination |
| UI | 360dp, 600dp, 840dp+; portrait/landscape; dark mode; 200% font; TalkBack; large lists |
| Readers | Missing PDF/EPUB handler, image decoding failures, unsupported video codec, pause on background |
| Share | Read-only URI grant to chosen receiver; Kindle share target available/unavailable |
| Widget | Supported launcher add/configure/open/delete, permission revoke, unsupported Fire launcher |

## ADB manual smoke procedure

Use a disposable test folder, never personal files. Install the debug APK; note `adb shell wm size`, then use UI Automator bounds to drive taps. Store captures under ignored `artifacts/`.

1. Launch `io.github.guardyanz.kindlefiles/.MainActivity` and connect a prepared test folder.
2. Search for a known file; toggle hidden files; change sorting; open Details.
3. Add a favorite and relaunch; verify the folder remains accessible.
4. Copy then move a test file into another granted tree. Compare hashes independently.
5. Select two files, create a ZIP, extract it into a fresh folder, and compare bytes.
6. Scan duplicate files; cancel deletion once, then confirm deletion of one disposable copy.
7. Preview a JPEG/video, open a PDF/EPUB, and cancel a share chooser.
8. Revoke a grant/remove a test drive and verify actionable errors.

Do not claim store readiness until this matrix is completed on actual target tablets.
