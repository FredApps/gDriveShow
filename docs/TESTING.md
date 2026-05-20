# Testing

## Local OAuth Client ID

For local sign-in testing, keep `app/src/main/res/values/oauth.xml` blank and provide the client ID outside source control.

Use one of these options:

```powershell
$env:GOOGLE_OAUTH_TV_CLIENT_ID="000000000000-example.apps.googleusercontent.com"
.\build-debug.ps1
```

or copy the example file:

```powershell
Copy-Item oauth.local.properties.example oauth.local.properties
notepad oauth.local.properties
.\build-debug.ps1
```

`oauth.local.properties` is ignored by Git. It should contain:

```properties
googleOAuthTvClientId=000000000000-example.apps.googleusercontent.com
```

The Settings screen shows `OAuth client: Configured` when the build contains a client ID. If it shows `Missing`, the Connect button stays disabled.

## Device Sign-In Smoke Test

1. Build and install the debug APK.
2. Open `Settings`.
3. Confirm `OAuth client` says `Configured`.
4. Select `Connect`.
5. Open the shown Google URL on a phone or computer.
6. Enter the user code and approve Drive access.
7. Return to the TV and select `Check approval`.
8. Confirm the Drive section loads real folders/images/videos instead of sample content.

## Local Verification Commands

```powershell
$env:JAVA_HOME=(Join-Path (Get-Location) '.tools\jdk')
$env:ANDROID_SDK_ROOT=(Join-Path (Get-Location) '.tools\android-sdk')
$env:ANDROID_HOME=$env:ANDROID_SDK_ROOT
.\.tools\gradle\bin\gradle.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease --no-daemon
```
