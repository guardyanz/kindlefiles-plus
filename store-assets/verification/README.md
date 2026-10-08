# Expressive UI verification

These unedited ADB captures document the signed `0.9.0-rc2` candidate. They use disposable sample files on AOSP API 28 emulators. The [workflow run](https://github.com/guardyanz/kindlefiles-plus/actions/runs/37708694314) records both device jobs. APK hashes are recorded in [tablet capture.json](../screenshots/capture.json) and [compact-capture.json](compact-capture.json).

| Capture | Check |
| --- | --- |
| [qa-dark-places.png](qa-dark-places.png) | Dark background, header, sidebar and glass hero |
| [qa-dark-browser.png](qa-dark-browser.png) | Dark search field, headings and file rows |
| [qa-compact-places.png](qa-compact-places.png) | Native 800x1280 portrait at 400x640dp |
| [qa-compact-favorites.png](qa-compact-favorites.png) | Favorite rows and floating bottom navigation |
| [qa-compact-large-text.png](qa-compact-large-text.png) | Favorites and navigation at 200% font size |
| [qa-compact-tools.png](qa-compact-tools.png) | Scrollable tools and scan action at 200% font size |

The tablet job uses a Nexus 10 profile at 2560x1600 and 320 dpi. The compact job uses a Nexus 7 profile at its native portrait resolution, also at 320 dpi. Both jobs operate the actual system folder picker and app controls using fresh UI Automator bounds.

The private [rc2 release](https://github.com/guardyanz/kindlefiles-plus/releases/tag/v0.9.0-rc2) includes `folderkit-motion.mp4`, a native screen recording of navigation and favorite interactions with Android animation scales set to 1. Still screenshots use disabled animations. The custom UI effects also observe disabled system animations while the app is open.

[contrast.json](contrast.json) records calculations for ten palette combinations, including alpha-composited glass fills. The minimum measured ratio is 6.70:1. These values do not establish complete accessibility conformance; TalkBack and actual Fire hardware checks remain in [TESTING.md](../../docs/TESTING.md).

The reference image supplied for the redesign informed color, rounded surfaces and layered shapes. No reference photographs, logo or interface assets are packaged. These captures and original sample documents are distributed under the project's Apache-2.0 license.
