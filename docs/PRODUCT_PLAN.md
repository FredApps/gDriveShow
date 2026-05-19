# Product Plan

## Vision

gDriveShow turns a TV into a calm, reliable display surface for personal Google Drive media. The app should feel like a native living-room browser first, then a slideshow appliance once content starts playing.

## Primary Users

- A home user who wants family photos and videos on an Android TV.
- A small venue or office that wants a simple rotating Drive-backed display.
- A technically comfortable owner who can authorize Google Drive once and then use a remote control day to day.

## MVP Scope

1. Android TV shell
   - Leanback launcher support.
   - D-pad-first browse screen.
   - Fullscreen slideshow screen.

2. Google Drive connection
   - OAuth sign-in.
   - Folder picker rooted in the user's Drive.
   - Read-only file metadata loading.

3. Media browsing
   - Folder grid.
   - Image and video tiles.
   - Sorting by name and modified date.
   - Basic search within the selected Drive folder.

4. Display modes
   - Image slideshow with configurable duration.
   - Video playback.
   - Mixed folder mode that alternates images and videos.

## Later Scope

- Shared drive support.
- Folder pinning for quick launch.
- Offline metadata cache and thumbnail cache.
- Exclude/include filters by MIME type.
- Ambient schedule rules.
- Remote-friendly settings export/import.

## Technical Direction

- UI: Kotlin + Jetpack Compose.
- App shell: Android TV leanback launcher intent.
- Drive boundary: repository interface, so UI can be developed against sample data.
- Media playback: add Media3/ExoPlayer when playback work begins.
- Auth: keep tokens in Android account/credential storage; never store plain tokens.

## Milestones

### Milestone 1: TV Shell

- App launches from Android TV home.
- Browse screen shows folders/media from local sample data.
- Slideshow preview screen works with D-pad controls.

### Milestone 2: Drive Read Access

- User can sign in.
- App lists Drive folders and supported media files.
- Browse screen consumes real Drive data.

### Milestone 3: Playback

- Images load full-screen.
- Videos play with remote controls.
- Mixed slideshow handles unsupported files gracefully.

### Milestone 4: Polish

- Persistent selected folder.
- Settings screen.
- Loading, empty, and error states.
- TV-safe typography and focus QA.

