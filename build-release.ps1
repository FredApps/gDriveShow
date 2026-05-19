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

& $Gradle -p $ProjectRoot :app:assembleRelease --no-daemon
