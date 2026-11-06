# Contributing to FolderKit

FolderKit is an independent Apache-2.0 project for Android and Fire tablets.

## Build and verify

Install JDK 17+, Android SDK platform 36 and build tools 35.0.0. Set `ANDROID_HOME` or create an ignored `local.properties` containing your SDK path.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Use `gradlew.bat` on Windows. No signing key, Amazon account, Google Play Services or backend is needed for a debug build.

## Changes that fit

- Reproducible Storage Access Framework provider failures, with disposable fixture files.
- Physical Fire tablet testing against [the device matrix](docs/TESTING.md).
- Accessibility, translations and focused UI improvements.
- Streaming file-engine regressions that preserve source files and reject overwrites.

Keep changes small and explain the behavior before and after. Add a regression test for file-operation behavior. Include screenshots for UI changes and say whether they came from AOSP or physical Fire hardware.

Do not import GPL code or assets from other file managers. New code and original assets use Apache-2.0; keep third-party notices. Never commit signing keys, passwords, `local.properties`, release-secrets or personal test data.

## Reporting bugs

Use the issue template with app version, device, OS, storage provider and steps using disposable files. For a possible security issue, use the repository's private vulnerability reporting form instead of posting sensitive details in a public issue.
