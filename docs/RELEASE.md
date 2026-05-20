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

### Signing Keystore Location

The keystore lives in OneDrive alongside the other FredApps secrets:

```
%USERPROFILE%\OneDrive\Projects\.secrets\gDriveShow-release.jks
%USERPROFILE%\OneDrive\Projects\.secrets\gDriveShow-release.password.txt
```

Key alias: `gdriveshow`

### Local Signed Build (auto-wired)

`build-release.ps1` automatically picks up the keystore from the secrets folder above when
`GDRIVESHOW_KEYSTORE` is not already set. Just run:

```powershell
.\build-release.ps1
```

The signed APK will be written to `app/build/outputs/apk/release/app-release.apk`.

### Creating the Keystore (one-time, already done)

If you ever need to recreate the keystore from scratch:

```powershell
& .\.tools\jdk\bin\keytool.exe -genkeypair `
  -v `
  -keystore "$env:USERPROFILE\OneDrive\Projects\.secrets\gDriveShow-release.jks" `
  -alias gdriveshow `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Store the chosen passwords in `gDriveShow-release.password.txt` (raw password, no newline).

### CI / Manual Override

To override signing credentials explicitly (e.g. in a CI environment):

```powershell
$env:GDRIVESHOW_KEYSTORE          = "C:\path\to\gDriveShow-release.jks"
$env:GDRIVESHOW_KEYSTORE_PASSWORD = "your-store-password"
$env:GDRIVESHOW_KEY_ALIAS         = "gdriveshow"
$env:GDRIVESHOW_KEY_PASSWORD      = "your-key-password"
.\build-release.ps1
```

### Step 3: Install on Android TV

To install on an Android TV device or emulator:

```powershell
& .\.tools\android-sdk\platform-tools\adb.exe install -r app/build/outputs/apk/release/app-release.apk
```

The `-r` flag replaces any existing installation.

## Security

- **Never commit the keystore file** to Git — it lives in OneDrive, not the repo.
- **Never commit keystore passwords** to source control.
- Keep the OAuth client ID and signing credentials separate from Git.
