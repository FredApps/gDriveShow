# Google Drive Auth Strategy

## Recommended Flow

Use Google's OAuth 2.0 flow for TVs and limited-input devices. The TV app requests a device code, displays a user code plus verification URL, and polls for the resulting access token after the user approves on a phone or computer.

Google docs:

- https://developers.google.com/identity/protocols/oauth2/limited-input-device
- https://developers.google.com/workspace/drive/api/guides/search-files
- https://developers.google.com/workspace/drive/api/guides/about-files

## Why This Fits

- Android TV remote input is awkward for email/password or browser-heavy sign-in.
- Google explicitly documents this flow for TVs and limited-input devices.
- The app can request read-only Drive scopes and keep the daily TV experience remote-friendly.

## App Flow

1. User opens Settings > Drive > Connect.
2. App requests a device code from Google.
3. App shows the code and verification URL full-screen.
4. User approves on another device.
5. App polls at Google's returned interval.
6. App stores refresh/access credentials in Android private credential storage.
7. `DriveRepository` switches from sample data to real Drive data.

## Drive Listing Query

The first Drive-backed repository should request only the fields needed by the UI:

```text
files(id,name,mimeType,modifiedTime,thumbnailLink,videoMediaMetadata,durationMillis),nextPageToken
```

Initial server query should stay conservative:

```text
trashed = false and '<folderId>' in parents
```

Then filter app-side for folders plus MIME types starting with `image/` or `video/`. If that becomes too broad or slow, enumerate the specific image/video MIME types we want to support.

## Code Boundaries

- `DriveAuthClient` owns device-code authorization and token refresh.
- `DriveRepository` owns Drive file listing and maps metadata into `DriveItem`.
- `DriveFileMapper` keeps Google API DTOs out of Compose UI.
- Compose screens consume `DriveConnectionState` and `DriveContentState`.

## Open Setup Items

- Create a Google Cloud OAuth client of type `TVs and Limited Input devices`.
- Enable the Google Drive API.
- Decide final scope. Prefer the narrowest read-only scope that supports browsing the intended Drive content.
- Add secure token storage before any real credential is persisted.
