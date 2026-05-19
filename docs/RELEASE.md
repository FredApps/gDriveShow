# Release Build

`build-release.ps1` uses the project-local `.tools` JDK, Android SDK, and Gradle install, then runs `:app:assembleRelease`.

Without signing environment variables, Gradle still assembles the release variant for validation. For an installable signed APK, set these variables before running the script:

```powershell
$env:GDRIVESHOW_KEYSTORE="C:\path\to\gdriveshow-release.jks"
$env:GDRIVESHOW_KEYSTORE_PASSWORD="store-password"
$env:GDRIVESHOW_KEY_ALIAS="gdriveshow"
$env:GDRIVESHOW_KEY_PASSWORD="key-password"
.\build-release.ps1
```

Create a local upload key when needed:

```powershell
& .\.tools\jdk\bin\keytool.exe -genkeypair `
  -v `
  -keystore C:\path\to\gdriveshow-release.jks `
  -alias gdriveshow `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000
```

Do not commit the keystore or signing passwords. Keep the OAuth client ID and signing credentials separate from source control unless you intentionally move them into a secure build secret store.
