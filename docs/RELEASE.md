# Release Build and TV Installation

## Overview

`build-release.ps1` builds a release variant of the app using the local `.tools` JDK, Android SDK, and Gradle. You can build an unsigned APK for testing, or a signed APK for distribution.

## Building an Unsigned Release APK

For testing on a device you control (without signing):

```powershell
.\build-release.ps1
```

Gradle will assemble the release variant and output an unsigned APK to `app/build/outputs/apk/release/`.

## Building a Signed Release APK

For installation on TVs or for distribution, you need a signing key. The app will only install on devices if the APK is signed with the same key.

### Step 1: Create a Signing Keystore (One Time)

If you don't already have a keystore, create one:

```powershell
& .\.tools\jdk\bin\keytool.exe -genkeypair `
  -v `
  -keystore C:\path\to\gdriveshow-release.jks `
  -alias gdriveshow `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

You will be prompted to create passwords and enter certificate details. Keep the keystore file and passwords secure—**do not commit them to source control**.

### Step 2: Build with Your Signing Key

Before building, set these environment variables to your keystore details:

```powershell
$env:GDRIVESHOW_KEYSTORE = "C:\path\to\gdriveshow-release.jks"
$env:GDRIVESHOW_KEYSTORE_PASSWORD = "your-store-password"
$env:GDRIVESHOW_KEY_ALIAS = "gdriveshow"
$env:GDRIVESHOW_KEY_PASSWORD = "your-key-password"
.\build-release.ps1
```

The signed APK will be written to `app/build/outputs/apk/release/app-release.apk`.

### Step 3: Install on Android TV

To install on an Android TV device or emulator:

```powershell
& .\.tools\android-sdk\platform-tools\adb.exe install -r app/build/outputs/apk/release/app-release.apk
```

The `-r` flag replaces any existing installation.

## Security

- **Never commit the keystore file** to Git—add it to `.gitignore` if needed.
- **Never commit keystore passwords** to source control.
- Keep the OAuth client ID and signing credentials separate from Git.
- Consider storing these secrets in a secure build secret manager.
