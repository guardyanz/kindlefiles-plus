# Amazon Appstore submission pack

Status: **release candidate only**. Do not submit until the physical Fire tablet checks and screenshots below are complete. Repository name and Android package remain `kindlefiles-plus` / `io.github.guardyanz.kindlefiles`; customer-facing brand is FolderKit.

## Listing fields

Publisher: Ribnik Studio  
Display title: FolderKit for Fire tablets  
Category: Utilities  
Pricing: Paid, USD 1.99 base price (confirm banking/tax details in Developer Console)  
Support email: wardiyanto.skm@gmail.com

Short description:

> Organize files on your Fire tablet, SD card, or USB drive. Copy and move folders, create and extract ZIP archives, find duplicates, and open books with installed readers.

Long description (plain text):

> FolderKit brings your chosen folders together in one place. Connect internal storage, an SD card, or a USB drive using the Android system picker, then browse, search the current folder, sort, and keep favorite folders close.
>
> Copy or move selected files and folders with a read-back check before a move removes its source. Create ZIP archives from files and folders, or extract an archive into a new folder. Find identical files by SHA-256 and decide what to delete yourself. Preview images and videos, and open PDF or EPUB files with a compatible installed reader. Use Android's Share menu to send a selected file to another app, including Kindle when it is available.
>
> FolderKit works without an account, advertising, analytics, or internet permission. It can access only locations you choose in the system picker. Some protected Android folders cannot be selected. Long operations require the app to stay open; a canceled or interrupted batch may leave already-completed items in place. Folder shortcuts depend on launcher support.
>
> This app was not created or endorsed by Amazon.

Feature bullets:

1. Browse internal, SD, and USB folders exposed by the system picker.
2. Copy and move files or folders with destination verification.
3. Create and extract ZIP archives.
4. Review SHA-256 duplicate groups before deleting anything.
5. Preview media and open PDF or EPUB with an installed reader.

Suggested keywords: file manager, folders, SD card, USB, ZIP, duplicate files, PDF, EPUB

Target audience: general utility; not designed for children. Complete the content rating and age fields accurately in Developer Console. User data privacy: based on the current binary, the app does not collect user data or transfer it to the publisher or third parties; user-initiated sharing to another app is a deliberate user action. Recheck this answer for each release and answer the console questionnaire. [Privacy policy](PRIVACY.md) should be publicly hosted before submission even if the console does not request its URL for this answer.

## Assets

- `store-assets/folderkit-114.png` and `store-assets/folderkit-512.png` are original transparent PNGs in Amazon's required dimensions. The source is `scripts/Export-StoreIcons.py`.
- **Still required:** 3–10 real screenshots from the final app on a Fire tablet. Use 1280×800, 1920×1200, or another [allowed resolution](https://developer.amazon.com/docs/app-submission/appstore-details.html); show Places, a connected folder, and ZIP/duplicates with disposable sample files. Do not fabricate Fire screenshots or show personal documents.
- Promotional image/video are optional and intentionally omitted.

## Submission gate

1. Back up the signing key and password from `release-secrets/` in a separate secure location. Never commit them.
2. Complete the [physical Fire device acceptance matrix](TESTING.md), especially SAF on Fire OS, SD/USB OTG, unplug and low-space errors, PDF/EPUB handlers, widget support, and accessibility. Amazon's [presubmission checklist](https://developer.amazon.com/docs/app-submission/presubmission-checklist.html) explicitly requires testing on a physical device.
3. Capture the required real screenshots and host the privacy policy at a public HTTPS URL. Confirm the support email is monitored.
4. In the Amazon Developer Console, supply publisher identity, payment/banking/tax details for a paid app, content rating, privacy response, supported Fire tablets, the listing fields above, icons/screenshots, and the signed APK. Exclude Fire TV devices. Review Amazon's DRM choice and test the resulting paid install.
5. Build the final binary from a reviewed commit, verify its SHA-256 and signature, then record the exact submitted version code and certificate SHA-256. Do not change the signing key for subsequent updates.

The signed `0.9.0-rc1` APK is a private test candidate. After physical validation, advance `versionCode` and `versionName`, rerun checks, and sign the final build.
