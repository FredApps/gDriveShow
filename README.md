# gDriveShow

Android TV app for browsing and displaying Google Drive images and videos from the couch.

## Goals

- Browse Google Drive folders, images, and videos with D-pad-friendly navigation.
- Play videos and preview images from a polished TV-first surface.
- Run image slideshows with configurable timing and playback controls.
- Keep Drive access, playback, and UI separated so the app can grow without becoming tangled.

## Initial Shape

- `app/` contains the Android TV application scaffold.
- `docs/PRODUCT_PLAN.md` captures the staged product plan.
- `docs/DESIGN.md` captures the first UX and visual design direction.

The app now keeps sample content for unsigned/demo runs, but switches to Google Drive content when OAuth tokens are present.

## Google Drive Auth

The project includes a compile-ready OAuth device-code client and encrypted token store. To use real Drive auth, create a Google OAuth client of type `TVs and Limited Input devices`, then put the client ID in:

```text
app/src/main/res/values/oauth.xml
```

The Settings screen already includes the TV-friendly connect flow. With the client ID filled in, it can request a code, poll for approval, and store tokens.

When stored tokens exist, the app attempts to load supported folders, images, videos, and shared drives from Google Drive and supports folder navigation. Without tokens, it falls back to sample data.

The current folder can be saved as the startup folder from the Browse screen. Settings also includes a startup folder picker using the current folder and discovered child folders.

Images and videos can open in a fullscreen viewer with previous/next navigation. Real Drive images and thumbnails are loaded with authenticated requests, and videos use Media3/ExoPlayer with Drive authorization headers when media URLs are available.

The fullscreen viewer handles TV remote left/right/back/select keys and shows video buffering, ready, playing, paused, ended, and error states. The slideshow screen now auto-advances images and videos, supports pause/resume, and plays mixed Drive media.

Drive folder metadata is cached locally after successful loads and reused if the next Drive request fails. Drive thumbnails are cached in the app cache directory after first load.

## Local Setup

This local checkout includes a copied `.tools` folder from `WatchTalk` for JDK, Android SDK, and Gradle. The folder is intentionally ignored by Git because it is large machine-local tooling.

Build locally with:

```powershell
.\build-debug.ps1
```

Run unit tests with:

```powershell
$env:JAVA_HOME=(Join-Path (Get-Location) '.tools\jdk')
$env:ANDROID_SDK_ROOT=(Join-Path (Get-Location) '.tools\android-sdk')
$env:ANDROID_HOME=$env:ANDROID_SDK_ROOT
.\.tools\gradle\bin\gradle.bat :app:testDebugUnitTest --no-daemon
```

Build a release APK with:

```powershell
.\build-release.ps1
```

For a signed release, set `GDRIVESHOW_KEYSTORE`, `GDRIVESHOW_KEYSTORE_PASSWORD`, `GDRIVESHOW_KEY_ALIAS`, and `GDRIVESHOW_KEY_PASSWORD` before running the release script. See `docs/RELEASE.md`.

## Repository

Private GitHub repository: https://github.com/FredApps/gDriveShow
