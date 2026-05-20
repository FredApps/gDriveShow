# Contributing

Thanks for taking a look at gDriveShow.

This app is entirely untested, so contributions should be small, easy to review, and honest about risk.

## Before Opening a Pull Request

- Do not include secrets, OAuth tokens, keystores, APKs, AABs, or local machine paths.
- Keep changes focused on one feature, fix, or documentation improvement.
- Run the most relevant local check you can.
- Mention any checks you could not run.

## Local Checks

Build a debug APK:

```powershell
.\build-debug.ps1
```

Run unit tests:

```powershell
$env:JAVA_HOME = (Join-Path (Get-Location) '.tools\jdk')
$env:ANDROID_SDK_ROOT = (Join-Path (Get-Location) '.tools\android-sdk')
$env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
.\.tools\gradle\bin\gradle.bat :app:testDebugUnitTest --no-daemon
```

## Licensing

By contributing, you agree that your contribution is licensed under GPLv3.
