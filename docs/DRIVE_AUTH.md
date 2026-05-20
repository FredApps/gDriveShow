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

## Current Code

- `GoogleDeviceCodeAuthClient` implements the device-code request and token polling calls.
- `EncryptedDriveTokenStore` stores OAuth tokens with AndroidX encrypted shared preferences.
- `DriveAuthConfig` holds the OAuth client ID and requested scopes.
- Local builds can inject the OAuth client ID with `GOOGLE_OAUTH_TV_CLIENT_ID`, Gradle property `googleOAuthTvClientId`, or ignored `oauth.local.properties`.
- `google_oauth_tv_client_id` can remain blank in `app/src/main/res/values/oauth.xml` during local testing.
- Settings includes a remote-friendly Connect flow that shows the Google user code, verification URL, and approval-check action.
- `DriveAccessTokenProvider` reuses stored access tokens and refreshes them when they are close to expiry.
- `GoogleDriveRepository` lists Drive folders/media with `files.list`, includes all-drives flags, appends shared-drive root entries, and stores folder metadata in a local cache after successful loads.

After creating the Google Cloud OAuth client, provide the TV/limited-input client ID through the local build environment or, for a deliberately branded production build, through `google_oauth_tv_client_id`. Do not add a client secret; this flow is for installed devices that cannot keep secrets.

## Drive Listing Query

The first Drive-backed repository should request only the fields needed by the UI:

```text
nextPageToken,files(id,name,mimeType,modifiedTime,description,thumbnailLink,videoMediaMetadata)
```

Initial server query should stay conservative:

```text
'<folderId>' in parents and trashed = false
```

Then filter app-side for folders plus MIME types starting with `image/` or `video/`. If that becomes too broad or slow, enumerate the specific image/video MIME types we want to support.

## Code Boundaries

- `DriveAuthClient` owns device-code authorization and token refresh.
- `DriveRepository` owns Drive file listing and maps metadata into `DriveItem`.
- `DriveFileMapper` keeps Google API DTOs out of Compose UI.
- Compose screens consume `DriveConnectionState` and `DriveContentState`.
- `DriveMetadataCache` stores the last loaded folder metadata and lets the UI show cached content if a later network request fails.
- `DriveThumbnailCache` stores fetched Drive thumbnails in the app cache directory and is cleared on sign-out.

## Open Setup Items

- Create a Google Cloud OAuth client of type `TVs and Limited Input devices`.
- Enable the Google Drive API.
- Decide final scope. Prefer the narrowest read-only scope that supports browsing the intended Drive content.
- Replace the remaining sample account label with token-aware account/profile data.
- Provide the real OAuth client ID through `GOOGLE_OAUTH_TV_CLIENT_ID`, Gradle property `googleOAuthTvClientId`, `oauth.local.properties`, or the production resource value.
- Verify the Settings sign-in flow on an Android TV device or emulator.
