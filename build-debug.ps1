$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:JAVA_HOME = Join-Path $root ".tools\jdk"
$env:ANDROID_SDK_ROOT = Join-Path $root ".tools\android-sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:Path"

$localProperties = Join-Path $root "local.properties"
$sdkPath = ($env:ANDROID_SDK_ROOT -replace "\\", "\\")
"sdk.dir=$sdkPath" | Set-Content -LiteralPath $localProperties -Encoding ASCII

& (Join-Path $root ".tools\gradle\bin\gradle.bat") :app:assembleDebug --no-daemon

