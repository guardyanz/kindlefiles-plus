# FolderKit tablet screenshots

Seven unedited PNGs captured directly with ADB from the signed `0.9.0-rc2` APK. Device: AOSP Android emulator API 28, x86_64, Nexus 10 profile, 2560x1600 landscape, 320 dpi. These are Android emulator captures; no Fire OS device branding is added. All documents are original disposable samples, with no personal files.

| File | Scene |
| --- | --- |
| [01-places.png](01-places.png) | First launch and folder connection |
| [02-browser.png](02-browser.png) | Connected Reading folder, documents, EPUB and ZIP |
| [03-favorites.png](03-favorites.png) | Saved Reading and Books folders |
| [04-archive-actions.png](04-archive-actions.png) | ZIP extraction and file actions menu |
| [05-file-tools.png](05-file-tools.png) | Archive and duplicate tools |
| [06-duplicates.png](06-duplicates.png) | SHA-256 duplicate scan results |
| [07-selection.png](07-selection.png) | Selected file and transfer/archive actions |

[capture.json](capture.json) records the APK SHA-256 and image dimensions. The [capture workflow run](https://github.com/guardyanz/kindlefiles-plus/actions/runs/37708694314) ran on 2026-10-08. The source journey is [Capture-Screenshots.py](../../scripts/Capture-Screenshots.py); rerun the manual [workflow](../../.github/workflows/screenshots.yml) whenever the app UI changes. Images are distributed with this project's Apache-2.0 release material.

[Dark, compact and enlarged-text captures](../verification/README.md) are supplied as review evidence. The private [rc2 release](https://github.com/guardyanz/kindlefiles-plus/releases/tag/v0.9.0-rc2) includes the signed APK, screenshot ZIP and a native interaction recording.

The dimensions match Amazon's [tablet screenshot specifications](https://developer.amazon.com/docs/app-submission/appstore-details.html). Store acceptance and physical Fire compatibility remain separate submission checks in [APPSTORE.md](../../docs/APPSTORE.md).
