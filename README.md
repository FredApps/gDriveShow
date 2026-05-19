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


## Installation on Sony Android TV

### Prerequisites

- Sony Android TV (Android 5.0 or higher)
- - Developer Mode enabled on the TV
  - - USB cable or network connectivity for app transfer
   
    - ### Enabling Developer Mode
   
    - 1. From the TV remote, press the **Home** button to go to the home screen
      2. 2. Navigate to **Settings** > **About** > **Build** (the exact path may vary by TV model)
         3. 3. Press the OK button on the remote to select "Build" and scroll down to find **Build Number**
            4. 4. Press OK multiple times on **Build Number** until you see a message saying "Developer mode is enabled"
               5.    - Typically this requires 7-10 presses
                     - 5. Press the Back button to return, then navigate to **Settings** > **Developer Options**
                       6. 6. Enable **USB Debugging** and **ADB (Android Debug Bridge)** if available
                         
                          7. ### Installing the App
                         
                          8. #### Option 1: Via USB Debugging (Recommended)
                         
                          9. 1. Enable USB debugging on the TV (see above)
                             2. 2. Connect your TV to a computer using a USB cable
                                3. 3. On your computer, open a terminal/command prompt and run:
                                   4.    ```
                                            adb connect <TV_IP_ADDRESS>
                                            adb install path/to/gDriveShow.apk
                                            ```
                                         4. The app will install automatically
                                     
                                         5. #### Option 2: Via Network ADB
                                     
                                         6. 1. Find your TV's IP address in **Settings** > **Network** > **About**
                                            2. 2. On your computer, run:
                                               3.    ```
                                                        adb connect <TV_IP_ADDRESS>:5555
                                                        adb install path/to/gDriveShow.apk
                                                        ```

                                                     #### Option 3: Via USB Storage (if ADB not available)

                                                 1. Copy the APK file to a USB drive
                                                 2. 2. Connect the USB drive to your TV
                                                    3. 3. Use the TV's file manager to navigate to the USB drive and select the APK file
                                                       4. 4. The TV will prompt you to install the app
                                                          5. 5. Press OK to confirm the installation
                                                            
                                                             6. ### Launching the App
                                                            
                                                             7. 1. After installation, the app will appear in your **Apps** menu
                                                                2. 2. Use your TV remote to navigate to **Apps** > **gDriveShow**
                                                                   3. 3. Press OK to launch the app
                                                                      4. 4. On first launch, you may be prompted to grant permissions - allow access as needed
                                                                        
                                                                         5. ### Setting Up Google Drive Access
                                                                        
                                                                         6. 1. Once the app is open, navigate to **Settings**
                                                                            2. 2. Select **Google Account** and follow the OAuth flow to authorize access to your Google Drive
                                                                               3. 3. Select your startup folder from the Browse screen
                                                                                  4. 4. You can now browse and view your Google Drive content
                                                                                    
                                                                                     5. 
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
