# Working in KindleFiles+

- New application code and original assets are Apache-2.0. Do not import GPL code/assets from file-manager references.
- SAF is the storage boundary. No raw path access or broad storage permissions in the application.
- File I/O stays off the main thread. Preserve sources on failed transfers; never silently overwrite.
- Run `./gradlew :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` after functional changes.
- Keep real-device limitations and incomplete features explicit in README and docs/TESTING.md.
- When graphify-out/graph.json exists, use `graphify query "<question>"` before raw codebase searches; use `path` and `explain` for focused relationships.
- After changing code, run `graphify update .`. Generated graph output remains local.
