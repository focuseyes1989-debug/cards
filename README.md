# Invitation Gallery

Offline Android catalog for invitation-card shops, designed for tablets and landscape phones. Package: `com.invitation.gallery`.

## Features

- Pink catalog with categories, search, prices, design details and image/video galleries.
- Password-protected Admin with separate Cards, Categories and Settings pages; password setup/change, logout and background locking.
- One Title field, Design Code, category, colors, size, paper type, pricing mode, price note, Featured and Customer visibility.
- Cards show clickable invitation photo thumbnails, with video-only and missing-photo placeholders.
- Paginated Cards and Categories lists (12 per page), search, category/visibility/media filters, category card counts and direct links. Search, filters, page and scroll position are retained after editing.
- Android Gallery/photo picker for importing media into private app storage.
- ZIP backup/restore for the catalog and media. Each device keeps its own Admin password separately.
- Admin → **App update စစ်ရန်** checks this repository's latest GitHub Release, downloads a newer APK and opens Android's update installer.

## Build

Use JDK 21, Android SDK platform/build-tools 36 and PowerShell. Set `JAVA_HOME` and `ANDROID_HOME`, and set `sdk.dir` in an untracked `local.properties` if needed.

```powershell
.\build.ps1
```

The Gradle wrapper is included. Output: `dist/Invitation-Gallery.apk`. An optional ignored `.local-tools.ps1` can set tool paths on the developer's machine.

For browser regression tests, install Node.js dependencies and Edge:

```powershell
npm install
npm test
```

## Publish an app update

1. Increase the default `versionCode` and `versionName` in `app/build.gradle`.
2. Update `release-notes.md`, commit, and push source to `main`.
3. Sign with the original signing key used by the installed app. Preserve that key on the build machine; it is never committed to this repository. `release-signing.json` pins the expected public certificate fingerprint.
4. Authenticate the GitHub CLI with `gh auth login`, then run:

```powershell
.\publish-release.ps1
```

The script builds, verifies the signing certificate, generates `dist/update.json`, uploads the APK and manifest to a draft release, then publishes it as latest. It refuses to replace an already published version. A failed upload leaves a draft so users cannot receive an incomplete update.

The app checks:

`https://github.com/focuseyes1989-debug/cards/releases/latest/download/update.json`

The manifest contains schema, package name, version code/name, minimum SDK, version-specific APK URL, SHA-256, exact byte size, and release notes. The app accepts HTTPS GitHub release assets only. Before installation it verifies size, checksum, package name, increasing version code and the same signing certificate as the installed app.

On first in-app installation, Android may ask to enable **Allow from this source**. Return to the app and press Install again, then confirm Update in Android's installer. Catalog data, imported media and Admin password remain in place. The update feature requires Internet; browsing the existing catalog works offline.

## Storage and compatibility

Imported media and the catalog are private to the app. Export a backup before uninstalling or clearing app data. Backups do not include the Admin password or app updater files. Existing single and bilingual design titles migrate to one Title without dropping either language.

Android 8+ is supported. Android 13+ uses the platform photo picker; older versions use a Gallery picker. Current APKs are direct-install builds using the existing signing identity, without Play Store distribution.