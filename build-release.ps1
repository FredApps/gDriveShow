$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsRoot = Join-Path $ProjectRoot ".tools"
$Gradle = Join-Path $ToolsRoot "gradle\bin\gradle.bat"
$JavaHome = Join-Path $ToolsRoot "jdk"
$AndroidSdk = Join-Path $ToolsRoot "android-sdk"

if (-not (Test-Path $Gradle)) {
    throw "Gradle was not found at $Gradle. Copy .tools from WatchTalk first."
}

$env:JAVA_HOME = $JavaHome
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:ANDROID_HOME = $AndroidSdk

"sdk.dir=$($AndroidSdk -replace '\\', '\\')" | Set-Content -Path (Join-Path $ProjectRoot "local.properties") -Encoding ASCII

# Auto-load signing credentials from OneDrive secrets if not already set by CI.
$SecretsDir = Join-Path $env:USERPROFILE "OneDrive\Projects\.secrets"
$DefaultKeystore = Join-Path $SecretsDir "gDriveShow-release.jks"
$DefaultPasswordFile = Join-Path $SecretsDir "gDriveShow-release.password.txt"

if (-not $env:GDRIVESHOW_KEYSTORE -and (Test-Path $DefaultKeystore)) {
    $env:GDRIVESHOW_KEYSTORE = $DefaultKeystore
    $env:GDRIVESHOW_KEY_ALIAS = "gdriveshow"
    if (Test-Path $DefaultPasswordFile) {
        $pass = (Get-Content $DefaultPasswordFile -Raw).Trim()
        $env:GDRIVESHOW_KEYSTORE_PASSWORD = $pass
        $env:GDRIVESHOW_KEY_PASSWORD = $pass
    }
}

& $Gradle -p $ProjectRoot :app:assembleRelease --no-daemon
