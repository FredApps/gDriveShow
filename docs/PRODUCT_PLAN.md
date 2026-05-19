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

- App launches from Android TV home. Done.
- Browse screen shows folders/media from local sample data. Done.
- Slideshow preview screen works with D-pad controls. Done.
- Section navigation for Drive, Slideshows, and Settings. Done.
- Browse filters and sorting over sample media. Done.

### Milestone 2: Drive Read Access

- User can sign in.
- App lists Drive folders and supported media files.
- Browse screen consumes real Drive data.
- Replace `SampleDriveRepository` with a Drive-backed implementation behind the existing `DriveRepository` interface.
- Add connected, disconnected, loading, empty, and error states to the app shell.
- Keep the sample repository available for emulator/demo builds.

### Milestone 3: Playback

- Images load full-screen. Authenticated Drive image byte loading done.
- Videos play with remote controls. Media3 playback with authenticated Drive requests started.
- Mixed slideshow handles unsupported files gracefully.

### Milestone 4: Polish

- Persistent selected folder.
- Settings screen.
- Loading, empty, and error states.
- TV-safe typography and focus QA.

## Current Implementation Notes

- The app has a project-local toolchain copied from `WatchTalk` in `.tools`.
- `build-debug.ps1` builds the debug APK with that local JDK, Gradle, and Android SDK.
- `DriveRepository` is the boundary for real Google Drive data.
- `SampleDriveRepository` currently feeds the TV shell, filters, slideshow library, and slideshow preview.

## Next Engineering Slice

1. Add app state models for Drive connection status and repository loading. Done.
2. Add Google API dependencies and an auth strategy suitable for Android TV. Auth strategy documented; OAuth client implemented with platform networking.
3. Implement a read-only Drive repository that maps Google Drive file metadata to `DriveItem`. Mapper boundary started.
4. Add loading, empty, and auth-error screens before wiring full media playback. Done.
5. Add secure token storage and a concrete `DriveAuthClient`. Done.
6. Wire Settings > Drive > Connect to `GoogleDeviceCodeAuthClient`. Done.
7. Add a Drive-backed repository that uses stored tokens to list real Drive files. Done.
8. Replace the sample connection state with token-aware connected/disconnected state. Started.
9. Add folder navigation beyond the Drive root folder. Done.
10. Add persisted folder selection and a folder picker for choosing the startup folder. Startup folder persistence done; full picker still pending.
11. Add media playback with image viewing and video playback. Viewer/control shell done.
12. Add authenticated media stream loading for Drive images and videos. Started.
13. Add playback error states, buffering indicators, and TV remote key handling.
