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

The first implementation uses sample content and domain models so the browse/slideshow experience can be designed before Google Drive auth and media streaming are wired in.

## Google Drive Auth

The project includes a compile-ready OAuth device-code client and encrypted token store. To use real Drive auth, create a Google OAuth client of type `TVs and Limited Input devices`, then put the client ID in:

```text
app/src/main/res/values/oauth.xml
```

The Settings screen already includes the TV-friendly connect flow. With the client ID filled in, it can request a code, poll for approval, and store tokens.

When stored tokens exist, the app attempts to load supported folders, images, and videos from Google Drive and supports folder navigation. Without tokens, it falls back to sample data.

## Local Setup

This local checkout includes a copied `.tools` folder from `WatchTalk` for JDK, Android SDK, and Gradle. The folder is intentionally ignored by Git because it is large machine-local tooling.

Build locally with:

```powershell
.\build-debug.ps1
```

## Repository

Private GitHub repository: https://github.com/FredApps/gDriveShow
