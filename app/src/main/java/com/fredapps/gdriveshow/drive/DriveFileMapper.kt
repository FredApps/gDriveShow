package com.fredapps.gdriveshow.drive

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val mimeType: String,
    val modifiedLabel: String,
    val description: String? = null,
    val itemCount: Int? = null,
    val durationLabel: String? = null,
    val mediaUrl: String? = null,
)

fun DriveFileMetadata.toDriveItem(): DriveItem {
    val type = when {
        mimeType == GoogleDriveMimeTypes.Folder -> DriveMediaType.Folder
        mimeType.startsWith("image/") -> DriveMediaType.Image
        mimeType.startsWith("video/") -> DriveMediaType.Video
        else -> DriveMediaType.Folder
    }

    return DriveItem(
        id = id,
        title = name,
        description = description ?: type.defaultDescription,
        type = type,
        itemCount = itemCount,
        durationLabel = durationLabel,
        modifiedLabel = modifiedLabel,
        accentColor = type.defaultAccentColor,
        mediaUrl = mediaUrl,
    )
}

object GoogleDriveMimeTypes {
    const val Folder = "application/vnd.google-apps.folder"
}

private val DriveMediaType.defaultDescription: String
    get() = when (this) {
        DriveMediaType.Folder -> "Google Drive folder."
        DriveMediaType.Image -> "Google Drive image."
        DriveMediaType.Video -> "Google Drive video."
    }

private val DriveMediaType.defaultAccentColor: Long
    get() = when (this) {
        DriveMediaType.Folder -> 0xFF72D6C9
        DriveMediaType.Image -> 0xFFFFD166
        DriveMediaType.Video -> 0xFFFF8A65
    }
