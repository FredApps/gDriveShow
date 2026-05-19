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

The project includes a compile-ready OAuth device-code client and encrypted token store. To use real Drive auth, create a Google OAuth client of type `TVs and Limited Input devices`.

For local testing, keep `app/src/main/res/values/oauth.xml` blank and provide the client ID with `GOOGLE_OAUTH_TV_CLIENT_ID`, Gradle property `googleOAuthTvClientId`, or ignored `oauth.local.properties`. See `docs/TESTING.md`.

The Settings screen already includes the TV-friendly connect flow. With the client ID filled in, it can request a code, poll for approval, and store tokens.

When stored tokens exist, the app attempts to load supported folders, images, videos, and shared drives from Google Drive and supports folder navigation. Without tokens, it falls back to sample data.

The current folder can be saved as the startup folder from the Browse screen. Settings also includes a startup folder picker using the current folder and discovered child folders.

Images and videos can open in a fullscreen viewer with previous/next navigation. Real Drive images and thumbnails are loaded with authenticated requests, and videos use Media3/ExoPlayer with Drive authorization headers when media URLs are available.

The fullscreen viewer handles TV remote left/right/back/select keys and shows video buffering, ready, playing, paused, ended, and error states. The slideshow screen now auto-advances images and videos, supports pause/resume, and plays mixed Drive media.

Drive folder metadata is cached locally after successful loads and reused if the next Drive request fails. Drive thumbnails are cached in the app cache directory after first load.


## Installation on Sony Android TV

### Prerequisites

- Sony Android TV running Android 5.0 or higher
- A computer with USB cable or network connectivity to transfer the APK
- Developer Mode and USB Debugging enabled on the TV

### Enabling Developer Mode

1. From your TV remote, press **Home** to go to the home screen
2. Navigate to **Settings** > **About** > **Build** (path may vary by TV model)
3. Press OK on **Build Number** repeatedly (typically 7-10 times) until you see "Developer mode is enabled"
4. Press Back to return to Settings, then navigate to **Settings** > **Developer Options**
5. Enable **USB Debugging** and **ADB (Android Debug Bridge)**

### Installing the App

After building the APK (see "Building a Release APK" above), you have three options:

#### Option 1: Network ADB (Recommended)

1. Find your TV's IP address: **Settings** > **Network** > **About**
2. On your computer, run:
   ```powershell
   & .\.tools\android-sdk\platform-tools\adb.exe connect <TV_IP_ADDRESS>:5555
   & .\.tools\android-sdk\platform-tools\adb.exe install app/build/outputs/apk/release/app-release.apk
   ```

#### Option 2: USB Cable

1. Connect your TV to your computer with a USB cable
2. On your computer, run:
   ```powershell
   & .\.tools\android-sdk\platform-tools\adb.exe connect <TV_IP_ADDRESS>
   & .\.tools\android-sdk\platform-tools\adb.exe install app/build/outputs/apk/release/app-release.apk
   ```

#### Option 3: USB Storage (if ADB unavailable)

1. Copy the APK file to a USB drive
2. Connect the USB drive to your TV
3. Use the TV's file manager to navigate to the USB drive and select the APK file
4. Press OK to confirm installation

### Launching and Setting Up

1. After installation, open the **Apps** menu on your TV
2. Select **gDriveShow** and press OK
3. On first launch, grant any requested permissions
4. Go to **Settings** > **Google Account** and authorize access to your Google Drive
5. Select your startup folder from the Browse screen
6. You're ready to browse and view your Google Drive content
## Local Setup

This checkout includes a local `.tools` folder containing the JDK, Android SDK, and Gradle. This folder is Git-ignored because it contains large machine-specific binaries.

### Building for Development

Build a debug APK that you can run on a device or emulator:

```powershell
.\build-debug.ps1
```

This script uses the local JDK, Android SDK, and Gradle to assemble the debug variant.

### Running Unit Tests

Set up the environment variables and run tests:

```powershell
$env:JAVA_HOME = (Join-Path (Get-Location) '.tools\jdk')
$env:ANDROID_SDK_ROOT = (Join-Path (Get-Location) '.tools\android-sdk')
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
.\.tools\gradle\bin\gradle.bat :app:testDebugUnitTest --no-daemon
```

### Building a Release APK

To build a release APK for installation on a TV or device:

```powershell
.\build-release.ps1
```

This creates an unsigned release APK suitable for testing on a device you control.

**For a signed release APK** (required for distribution), set these environment variables before running the script:

```powershell
$env:GDRIVESHOW_KEYSTORE = "C:\path\to\gdriveshow-release.jks"
$env:GDRIVESHOW_KEYSTORE_PASSWORD = "store-password"
$env:GDRIVESHOW_KEY_ALIAS = "gdriveshow"
$env:GDRIVESHOW_KEY_PASSWORD = "key-password"
.\build-release.ps1
```

For detailed signing and release instructions, see `docs/RELEASE.md`.

## Repository

Private GitHub repository: https://github.com/FredApps/gDriveShow
