# FolderKit tablet screenshots

Six unedited PNGs captured directly with ADB from the signed `0.9.0-rc1` APK. Device: AOSP Android emulator API 28, x86_64, Nexus 10 profile, 2560×1600 landscape, 320 dpi. These are Android emulator captures; no Fire OS device branding is added. All documents are original disposable samples, with no personal files.

| File | Scene |
| --- | --- |
| [01-places.png](01-places.png) | First launch and folder connection |
| [02-browser.png](02-browser.png) | Connected Reading folder, documents, EPUB and ZIP |
| [03-favorites.png](03-favorites.png) | Saved Reading and Books folders |
| [04-archive-actions.png](04-archive-actions.png) | ZIP extraction and file actions menu |
| [05-file-tools.png](05-file-tools.png) | Archive and duplicate tools |
| [06-duplicates.png](06-duplicates.png) | SHA-256 duplicate scan results |

[capture.json](capture.json) records the APK SHA-256 and image dimensions. The [capture workflow run](https://github.com/guardyanz/kindlefiles-plus/actions/runs/36827704863) succeeded on 2026-10-01. The source journey is [Capture-Screenshots.py](../../scripts/Capture-Screenshots.py); rerun the manual [workflow](../../.github/workflows/screenshots.yml) whenever the app UI changes. Images are distributed with this project's Apache-2.0 release material.

The dimensions match Amazon's [tablet screenshot specifications](https://developer.amazon.com/docs/app-submission/appstore-details.html). Store acceptance and physical Fire compatibility remain separate submission checks in [APPSTORE.md](../../docs/APPSTORE.md).
